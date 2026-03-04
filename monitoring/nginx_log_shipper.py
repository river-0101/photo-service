#!/usr/bin/env python3
"""
Nginx 로그를 NHN Cloud Log & Crash Search로 전송하는 스크립트.
cron: * * * * * /usr/bin/python3 /home/rocky/nginx_log_shipper.py
"""

import json
import os
import sys
import urllib.request

APPKEY = os.environ.get("LOG_CRASH_APPKEY", "")
API_URL = "https://api-logncrash.nhncloudservice.com/v2/log"
STATE_FILE = "/var/tmp/nginx_log_shipper.pos"
HOSTNAME = os.uname().nodename
BULK_SIZE = 50

LOG_FILES = [
    ("/var/log/nginx/access.log", "nginx-access"),
    ("/var/log/nginx/error.log", "nginx-error"),
]


def load_state():
    try:
        with open(STATE_FILE) as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        return {}


def save_state(state):
    with open(STATE_FILE, "w") as f:
        json.dump(state, f)


def send_bulk(logs):
    if not logs:
        return
    data = json.dumps(logs).encode("utf-8")
    req = urllib.request.Request(
        API_URL,
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        urllib.request.urlopen(req, timeout=10)
    except Exception:
        pass


def ship_log(log_file, log_type, last_inode, last_pos):
    try:
        stat = os.stat(log_file)
    except FileNotFoundError:
        return last_inode, last_pos

    current_inode = stat.st_ino

    # 로그 로테이션 감지 (inode 변경) → 처음부터 읽기
    if current_inode != last_inode:
        last_pos = 0

    # 파일이 줄어들었으면 (truncate) 처음부터
    if stat.st_size < last_pos:
        last_pos = 0

    # 새 내용이 없으면 스킵
    if stat.st_size == last_pos:
        return current_inode, last_pos

    bulk = []
    index = 0

    with open(log_file, "r", errors="replace") as f:
        f.seek(last_pos)
        for line in f:
            line = line.strip()
            if not line:
                continue
            index += 1
            bulk.append({
                "projectName": APPKEY,
                "projectVersion": "1.0.0",
                "logVersion": "v2",
                "body": line,
                "logSource": "nginx",
                "logType": log_type,
                "host": HOSTNAME,
                "lncBulkIndex": index,
            })
            if len(bulk) >= BULK_SIZE:
                send_bulk(bulk)
                bulk = []

        new_pos = f.tell()

    send_bulk(bulk)
    return current_inode, new_pos


def main():
    if not APPKEY:
        print("LOG_CRASH_APPKEY not set", file=sys.stderr)
        sys.exit(1)

    state = load_state()

    # 첫 실행이면 현재 위치부터 시작 (과거 로그 스킵)
    first_run = len(state) == 0

    for log_file, log_type in LOG_FILES:
        key = log_type
        if first_run:
            try:
                stat = os.stat(log_file)
                state[key] = {"inode": stat.st_ino, "pos": stat.st_size}
            except FileNotFoundError:
                state[key] = {"inode": 0, "pos": 0}
        else:
            prev = state.get(key, {"inode": 0, "pos": 0})
            new_inode, new_pos = ship_log(
                log_file, log_type, prev["inode"], prev["pos"]
            )
            state[key] = {"inode": new_inode, "pos": new_pos}

    save_state(state)

    if first_run:
        print("Initialized. Will start shipping from next run.")
    else:
        print("Done.")


if __name__ == "__main__":
    main()
