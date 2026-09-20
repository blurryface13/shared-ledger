import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import export_alert_state as state


class AlertStateTest(unittest.TestCase):
    def report(self, status, *codes):
        return {'checkedAt': '2026-09-20T16:00:00Z', 'status': status,
                'alerts': [{'code': code, 'value': 1} for code in codes]}

    def test_open_repeat_and_recovery(self):
        first, events = state.advance({}, self.report('attention', 'DEAD'), 'a')
        self.assertEqual('opened', events[0]['type'])
        second, events = state.advance(first, self.report('attention', 'DEAD'), 'a')
        self.assertEqual([], events)
        final, events = state.advance(second, self.report('healthy'), 'a')
        self.assertEqual('resolved', events[0]['type'])
        self.assertEqual({}, final['active'])

    def test_unknown_preserves_business_incident(self):
        first, _ = state.advance({}, self.report('attention', 'DEAD'), 'a')
        unknown, events = state.advance(first, self.report('unknown', 'PROBE_FAILED'), 'a')
        self.assertEqual({'DEAD', 'PROBE_FAILED'}, set(unknown['active']))
        self.assertEqual(['opened'], [event['type'] for event in events])
        restored, events = state.advance(unknown, self.report('attention', 'DEAD'), 'a')
        self.assertEqual({'DEAD'}, set(restored['active']))
        self.assertEqual('PROBE_FAILED', events[0]['code'])

    def test_new_target_cannot_resolve_other_target(self):
        old, _ = state.advance({}, self.report('attention', 'DEAD'), 'a')
        with self.assertRaises(ValueError):
            state.advance(old, self.report('healthy'), 'b')

    def test_persistence_survives_new_read(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'state.json'
            self.assertEqual(1, len(state.update(path, self.report('attention', 'DEAD'), 'a')))
            self.assertEqual([], state.update(path, self.report('attention', 'DEAD'), 'a'))
            self.assertEqual(0o600, path.stat().st_mode & 0o777)

    def test_corrupt_file_is_not_overwritten(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'state.json'
            path.write_text('broken')
            with self.assertRaises(ValueError):
                state.update(path, self.report('healthy'), 'a')
            self.assertEqual('broken', path.read_text())

    def test_concurrent_writer_cannot_modify_state(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'state.json'
            state.update(path, self.report('attention', 'DEAD'), 'a')
            before = path.read_text()
            with open(str(path) + '.lock', 'a') as lock:
                state.fcntl.flock(lock.fileno(), state.fcntl.LOCK_EX | state.fcntl.LOCK_NB)
                with self.assertRaises(BlockingIOError):
                    state.update(path, self.report('healthy'), 'a')
            self.assertEqual(before, path.read_text())

    def test_failed_replace_preserves_previous_state(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'state.json'
            state.update(path, self.report('attention', 'DEAD'), 'a')
            before = path.read_text()
            with patch.object(state.os, 'replace', side_effect=OSError('disk full')):
                with self.assertRaises(OSError):
                    state.update(path, self.report('healthy'), 'a')
            self.assertEqual(before, path.read_text())
            self.assertEqual(2, len(list(Path(directory).iterdir())))


if __name__ == '__main__':
    unittest.main()
