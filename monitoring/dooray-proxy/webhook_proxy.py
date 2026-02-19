import json
import os
import urllib.request
from http.server import HTTPServer, BaseHTTPRequestHandler

DOORAY_WEBHOOK_URL = os.environ.get("DOORAY_WEBHOOK_URL", "")
LISTEN_PORT = int(os.environ.get("LISTEN_PORT", "5001"))

SEVERITY_EMOJI = {"critical": "[CRITICAL]", "warning": "[WARNING]", "info": "[INFO]"}

def format_dooray_message(payload):
    alerts = payload.get("alerts", [])
    lines = []
    for alert in alerts:
        labels = alert.get("labels", {})
        annotations = alert.get("annotations", {})
        status = alert.get("status", "unknown").upper()
        severity = labels.get("severity", "info")
        emoji = SEVERITY_EMOJI.get(severity, "[INFO]")
        alertname = labels.get("alertname", "Unknown")
        summary = annotations.get("summary", "")
        description = annotations.get("description", "")
        lines.append(f"{emoji} {status} | {alertname}")
        if summary:
            lines.append(f"  {summary}")
        if description:
            lines.append(f"  {description}")
        lines.append("")
    return "\n".join(lines).strip() if lines else f"Grafana Alert"

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        body = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        try:
            grafana = json.loads(body)
        except json.JSONDecodeError:
            self.send_response(400)
            self.end_headers()
            return
        text = format_dooray_message(grafana)
        dooray = json.dumps({"botName": "Grafana Alert", "text": text}).encode("utf-8")
        req = urllib.request.Request(DOORAY_WEBHOOK_URL, data=dooray, headers={"Content-Type": "application/json"}, method="POST")
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                self.send_response(200)
        except Exception as e:
            print(f"Forward failed: {e}")
            self.send_response(502)
        self.end_headers()
    def log_message(self, fmt, *args):
        print(f"[proxy] {args[0]}")

if __name__ == "__main__":
    if not DOORAY_WEBHOOK_URL:
        print("ERROR: DOORAY_WEBHOOK_URL not set")
        exit(1)
    server = HTTPServer(("0.0.0.0", LISTEN_PORT), Handler)
    print(f"Webhook proxy on :{LISTEN_PORT} -> Dooray")
    server.serve_forever()
