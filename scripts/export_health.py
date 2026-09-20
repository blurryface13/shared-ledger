#!/usr/bin/env python3
"""Read-only local Docker export health probe. Emits JSON; never consumes queue messages."""
import argparse
import datetime
import json
import re
import subprocess
import sys

SQL = """
SELECT
 (SELECT COUNT(*) FROM export_outbox WHERE status='DEAD'),
 (SELECT COUNT(*) FROM export_outbox WHERE status='PENDING'),
 (SELECT COALESCE(MAX(TIMESTAMPDIFF(SECOND, created_at, NOW())),0) FROM export_outbox WHERE status='PENDING'),
 (SELECT COUNT(*) FROM tb_export_record WHERE export_status='FAILED'),
 (SELECT COUNT(*) FROM tb_export_record r LEFT JOIN export_outbox o ON o.export_record_id=r.id
  WHERE r.export_status='PENDING' AND o.export_record_id IS NULL),
 (SELECT COALESCE(MAX(TIMESTAMPDIFF(SECOND, r.created_at, NOW())),0)
  FROM tb_export_record r WHERE r.export_status IN ('PENDING','RUNNING'));
"""
FIELDS = ('dead', 'pending', 'oldest_pending_seconds', 'failed', 'legacy_pending', 'oldest_unfinished_seconds')


def run(command):
    # Discard raw stderr from reports: driver errors can contain credentials or addresses.
    result = subprocess.run(command, capture_output=True, text=True, timeout=15, check=True)
    return result.stdout


def collect(args):
    if not re.fullmatch(r'[A-Za-z0-9_]+', args.database):
        raise ValueError('Invalid database identifier')
    mysql = run(['docker', 'exec', args.mysql_container, 'sh', '-c',
                 'MYSQL_PWD="${MYSQL_MONITOR_PASSWORD:-$MYSQL_ROOT_PASSWORD}" exec mysql '
                 '--connect-timeout=5 -u"${MYSQL_MONITOR_USER:-root}" -N -B "$1" -e "$2"',
                 'export-health', args.database, SQL])
    values = mysql.strip().split('\t')
    if len(values) != len(FIELDS):
        raise ValueError('Unexpected database result')
    counts = dict(zip(FIELDS, map(int, values)))
    rows = json.loads(run(['docker', 'exec', args.rabbit_container, 'rabbitmqctl',
                          'list_queues', '-p', args.vhost, 'name', 'messages', 'consumers', '--formatter', 'json']))
    queues = {row['name']: row for row in rows}
    selected = {}
    for name in (args.queue, args.queue + '.failed'):
        if name not in queues:
            raise ValueError('Required queue missing')
        selected[name] = {key: int(queues[name][key]) for key in ('messages', 'consumers')}
    return counts, selected


def evaluate(counts, queues, queue, max_age, max_backlog):
    alerts = []
    def add(condition, code, value):
        if condition:
            alerts.append({'code': code, 'value': value})
    add(counts['dead'] > 0, 'OUTBOX_DEAD', counts['dead'])
    add(counts['failed'] > 0, 'EXPORT_FAILED', counts['failed'])
    add(counts['legacy_pending'] > 0, 'LEGACY_PENDING_WITHOUT_OUTBOX', counts['legacy_pending'])
    add(counts['pending'] > 0 and counts['oldest_pending_seconds'] >= max_age,
        'OUTBOX_PENDING_TOO_OLD', counts['oldest_pending_seconds'])
    add(counts['oldest_unfinished_seconds'] >= max_age, 'EXPORT_UNFINISHED_TOO_OLD', counts['oldest_unfinished_seconds'])
    add(queues[queue]['messages'] >= max_backlog, 'BUSINESS_QUEUE_BACKLOG', queues[queue]['messages'])
    add(queues[queue]['consumers'] == 0, 'BUSINESS_QUEUE_NO_CONSUMER', 0)
    add(queues[queue + '.failed']['messages'] > 0, 'FAILURE_QUEUE_NONEMPTY', queues[queue + '.failed']['messages'])
    return alerts


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mysql-container', default='trip-ledger-local-mysql')
    parser.add_argument('--rabbit-container', default='trip-ledger-rabbitmq')
    parser.add_argument('--database', default='db_trip_ledger')
    parser.add_argument('--queue', default='trip-ledger.export.queue')
    parser.add_argument('--vhost', default='/')
    parser.add_argument('--max-age-seconds', type=int, default=300)
    parser.add_argument('--max-backlog', type=int, default=100)
    args = parser.parse_args(argv)
    if args.max_age_seconds <= 0 or args.max_backlog <= 0:
        parser.error('Thresholds must be positive')
    report = {'checkedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'thresholds': {'maxAgeSeconds': args.max_age_seconds, 'maxBacklog': args.max_backlog}}
    try:
        counts, queues = collect(args)
        alerts = evaluate(counts, queues, args.queue, args.max_age_seconds, args.max_backlog)
        report.update(status='attention' if alerts else 'healthy', database=counts, queues=queues, alerts=alerts)
        code = 1 if alerts else 0
    except (subprocess.SubprocessError, OSError, ValueError, KeyError, TypeError) as exc:
        report.update(status='unknown', alerts=[{'code': 'PROBE_FAILED', 'type': type(exc).__name__}])
        code = 2
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return code


if __name__ == '__main__':
    sys.exit(main())
