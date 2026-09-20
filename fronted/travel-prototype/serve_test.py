"""Run with python3 -m unittest discover -s fronted/travel-prototype -p '*_test.py'."""
import http.client
import os
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from unittest.mock import patch
from serve import Handler

class ProxyTest(unittest.TestCase):
    def test_preserves_idempotency_key_and_body(self):
        received = {}
        class Backend(BaseHTTPRequestHandler):
            def do_POST(self):
                received['key'] = self.headers.get('Idempotency-Key')
                received['body'] = self.rfile.read(int(self.headers['Content-Length']))
                self.send_response(200)
                self.end_headers()
                self.wfile.write(b'{"code":0}')
            def log_message(self, *args): pass
        backend = ThreadingHTTPServer(('127.0.0.1', 0), Backend)
        proxy = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        workers = [threading.Thread(target=s.serve_forever, daemon=True) for s in (backend,proxy)]
        for t in workers: t.start()
        try:
            with patch.dict(os.environ, {'TRIP_LEDGER_API_PORT': str(backend.server_port)}):
                conn = http.client.HTTPConnection('127.0.0.1', proxy.server_port, timeout=3)
                try:
                    conn.request('POST','/api/v1/books/1/bills/personal-bill',b'{"amount":123}',
                                 {'Idempotency-Key':'test-stable-key-123','Content-Type':'application/json'})
                    response=conn.getresponse()
                    self.assertEqual(response.status,200)
                    response.read()
                finally: conn.close()
            self.assertEqual(received, {'key':'test-stable-key-123','body':b'{"amount":123}'})
        finally:
            for s in (proxy,backend): s.shutdown();s.server_close()
            for t in workers:t.join()
