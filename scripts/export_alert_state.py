"""Local incident transitions. No notifications and no background process."""
import fcntl
import json
import os
from pathlib import Path
import tempfile


def advance(previous, report, source):
    if previous and (previous.get('version') != 1 or previous.get('source') != source):
        raise ValueError('State schema or monitoring target mismatch')
    active = dict(previous.get('active', {})) if previous else {}
    now = report['checkedAt']
    current = {entry['code']: entry for entry in report['alerts']}
    events = []
    # Unknown observations cannot resolve previously observed business incidents.
    resolvable = set(active) - set(current) if report['status'] != 'unknown' else set()
    for code in sorted(resolvable):
        old = active.pop(code)
        events.append({'type': 'resolved', 'code': code, 'openedAt': old['openedAt'], 'at': now})
    for code, detail in sorted(current.items()):
        if code not in active:
            active[code] = {'openedAt': now}
            events.append({'type': 'opened', 'code': code, 'at': now})
        active[code].update(lastSeenAt=now, detail=detail)
    return {'version': 1, 'source': source, 'checkedAt': now, 'active': active, 'lastEvents': events}, events


def update(path, report, source):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    # Fixed sidecar inode: replacing the state file must not replace the lock.
    with open(str(path) + '.lock', 'a') as lock:
        fcntl.flock(lock.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
        previous = json.loads(path.read_text()) if path.exists() else {}
        state, events = advance(previous, report, source)
        temporary = None
        try:
            with tempfile.NamedTemporaryFile(mode='w', dir=path.parent, prefix=path.name + '.', delete=False) as stream:
                temporary = stream.name
                json.dump(state, stream, ensure_ascii=False, indent=2)
                stream.flush()
                os.fsync(stream.fileno())
            os.replace(temporary, path)
            temporary = None
        finally:
            if temporary:
                os.unlink(temporary)
    return events
