#!/usr/bin/env python3
"""
Serveur de controle distant - version 2 (sans ADB).
Transmet les commandes de la page web directement a l'app Android
installee sur la tablette (port 8080), via une simple requete HTTP.
Fonctionne entierement en local, sans Internet.
"""

import json
import os
import urllib.request
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse, parse_qs

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CONFIG_FILE = os.path.join(BASE_DIR, "config.json")
HTML_FILE = os.path.join(BASE_DIR, "remote_v2.html")
PORT = 8000
TABLET_APP_PORT = 8080  # port fixe de l'app Android installee sur la tablette


def load_config():
    if os.path.exists(CONFIG_FILE):
        with open(CONFIG_FILE) as f:
            return json.load(f)
    return {"tablet_ip": ""}


def save_config(cfg):
    with open(CONFIG_FILE, "w") as f:
        json.dump(cfg, f)


def call_tablet(action, timeout=4):
    cfg = load_config()
    ip = cfg.get("tablet_ip", "")
    if not ip:
        return False, "IP de la tablette non configuree"
    url = f"http://{ip}:{TABLET_APP_PORT}/cmd?action={action}"
    try:
        with urllib.request.urlopen(url, timeout=timeout) as resp:
            data = json.loads(resp.read().decode())
            return data.get("ok", False), None
    except Exception as e:
        return False, str(e)


class Handler(BaseHTTPRequestHandler):
    def _send_json(self, status, payload):
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(json.dumps(payload).encode())

    def _send_html(self):
        with open(HTML_FILE, "rb") as f:
            content = f.read()
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()
        self.wfile.write(content)

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        params = parse_qs(parsed.query)
        path = parsed.path

        if path in ("/", "/remote_v2.html"):
            self._send_html()
            return

        if path == "/status":
            cfg = load_config()
            self._send_json(200, {"ok": True, "tablet_ip": cfg.get("tablet_ip", "")})
            return

        if path == "/config":
            ip = params.get("ip", [None])[0]
            if ip:
                save_config({"tablet_ip": ip})
                self._send_json(200, {"ok": True})
            else:
                self._send_json(200, load_config())
            return

        if path == "/ping":
            ok, err = call_tablet("ping")
            # meme si "ping" n'est pas une vraie action, une reponse HTTP suffit a confirmer que l'app tourne
            self._send_json(200, {"ok": True})
            return

        if path == "/key":
            action = params.get("code", [""])[0]
            ok, err = call_tablet(action)
            self._send_json(200 if ok else 500, {"ok": ok, "error": err})
            return

        self._send_json(404, {"ok": False, "error": "route inconnue"})

    def log_message(self, format, *args):
        pass


if __name__ == "__main__":
    print(f"Serveur pret sur http://127.0.0.1:{PORT}")
    server = HTTPServer(("0.0.0.0", PORT), Handler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nServeur arrete.")
