"""Local UI/API gateway. Run Java on 8081; avoids browser cross-origin credentials."""
from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler
from pathlib import Path
import http.client
import os

class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(Path(__file__).parent), **kwargs)
    def proxy(self):
        size = int(self.headers.get('Content-Length', '0'))
        if size > 11 * 1024 * 1024:
            self.send_error(413); return
        headers = {k: v for k, v in self.headers.items() if k.lower() in ('authorization', 'content-type', 'accept')}
        connection = http.client.HTTPConnection('127.0.0.1', int(os.getenv('TRIP_LEDGER_API_PORT', '8081')), timeout=30)
        try:
            connection.request(self.command, self.path, self.rfile.read(size) if size else None, headers)
            response = connection.getresponse()
            payload = response.read()
            self.send_response(response.status)
            self.send_header('Content-Type', response.getheader('Content-Type', 'application/json'))
            self.send_header('Cache-Control', 'no-store')
            self.send_header('Content-Length', str(len(payload)))
            self.end_headers(); self.wfile.write(payload)
        except (OSError, http.client.HTTPException):
            self.send_error(502, 'Backend unavailable')
        finally: connection.close()
    def do_GET(self):
        if self.path.startswith('/api/v1/'): self.proxy()
        else: super().do_GET()
    def do_POST(self): self.api_only()
    def do_PUT(self): self.api_only()
    def do_DELETE(self): self.api_only()
    def do_PATCH(self): self.api_only()
    def api_only(self):
        if self.path.startswith('/api/v1/'): self.proxy()
        else: self.send_error(404)

if __name__ == '__main__':
    ThreadingHTTPServer(('127.0.0.1', int(os.getenv('PORT', '4178'))), Handler).serve_forever()
