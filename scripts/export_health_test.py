import contextlib
import io
import json
import subprocess
import unittest
from unittest.mock import patch
import export_health as health


class ExportHealthTest(unittest.TestCase):
    def setUp(self):
        self.counts = dict.fromkeys(health.FIELDS, 0)
        self.queues = {'q': {'messages': 0, 'consumers': 1}, 'q.failed': {'messages': 0, 'consumers': 0}}

    def test_healthy_empty_queues(self):
        self.assertEqual([], health.evaluate(self.counts, self.queues, 'q', 300, 100))

    def test_all_problem_classes_and_threshold_boundary(self):
        self.counts.update(dead=1, failed=2, legacy_pending=3, pending=1,
                           oldest_pending_seconds=300, oldest_unfinished_seconds=300)
        self.queues['q'].update(messages=100, consumers=0)
        self.queues['q.failed']['messages'] = 1
        self.assertEqual(8, len(health.evaluate(self.counts, self.queues, 'q', 300, 100)))

    def test_below_threshold_is_not_an_alert(self):
        self.counts.update(pending=1, oldest_pending_seconds=299, oldest_unfinished_seconds=299)
        self.queues['q']['messages'] = 99
        self.assertEqual([], health.evaluate(self.counts, self.queues, 'q', 300, 100))

    def test_probe_failure_is_unknown_and_redacts_error(self):
        with patch.object(health, 'collect', side_effect=subprocess.TimeoutExpired('secret-password', 15)):
            out = io.StringIO()
            with contextlib.redirect_stdout(out):
                self.assertEqual(2, health.main([]))
            self.assertEqual('unknown', json.loads(out.getvalue())['status'])
            self.assertNotIn('secret-password', out.getvalue())

    def test_alert_exit_code(self):
        self.counts['dead'] = 1
        queues = {'trip-ledger.export.queue': self.queues['q'], 'trip-ledger.export.queue.failed': self.queues['q.failed']}
        with patch.object(health, 'collect', return_value=(self.counts, queues)):
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(1, health.main([]))

    def test_missing_queue_is_not_healthy(self):
        with patch.object(health, 'run', side_effect=['0\t0\t0\t0\t0\t0\n', '[]']):
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(2, health.main([]))


if __name__ == '__main__':
    unittest.main()
