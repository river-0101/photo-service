#!/bin/bash
# nginx_log_shipper.sh
# Nginx 로그를 NHN Cloud Log & Crash Search로 전송하는 스크립트
# cron: * * * * * /home/rocky/nginx_log_shipper.sh (매분 실행)

set -euo pipefail

# === 설정 ===
APPKEY="${LOG_CRASH_APPKEY}"
API_URL="https://api-logncrash.nhncloudservice.com/v2/log"
STATE_FILE="/var/tmp/nginx_log_shipper.pos"
ACCESS_LOG="/var/log/nginx/access.log"
ERROR_LOG="/var/log/nginx/error.log"
HOSTNAME=$(hostname)
BULK_SIZE=50

# === 상태 파일 초기화 ===
if [ ! -f "$STATE_FILE" ]; then
    echo '{"access_lines":0,"error_lines":0}' > "$STATE_FILE"
fi

ACCESS_POS=$(python3 -c "import json; print(json.load(open('$STATE_FILE'))['access_lines'])")
ERROR_POS=$(python3 -c "import json; print(json.load(open('$STATE_FILE'))['error_lines'])")

# === 로그 라인을 JSON으로 변환하여 전송 ===
send_logs() {
    local log_file="$1"
    local log_type="$2"
    local start_line="$3"
    local total_lines
    total_lines=$(wc -l < "$log_file" 2>/dev/null || echo 0)

    # 로그 로테이션 감지 (현재 줄 수가 이전 위치보다 적으면 처음부터)
    if [ "$total_lines" -lt "$start_line" ]; then
        start_line=0
    fi

    local new_lines=$((total_lines - start_line))
    if [ "$new_lines" -le 0 ]; then
        echo "$total_lines"
        return
    fi

    # 새 로그 라인 추출
    local lines
    lines=$(tail -n +"$((start_line + 1))" "$log_file" | head -n "$new_lines")

    # Bulk 전송 (BULK_SIZE개씩 묶어서)
    local bulk_json="["
    local count=0
    local index=0

    while IFS= read -r line; do
        [ -z "$line" ] && continue
        index=$((index + 1))
        count=$((count + 1))

        # JSON 특수문자 이스케이프
        escaped_line=$(echo "$line" | python3 -c "import sys,json; print(json.dumps(sys.stdin.read().strip()))")

        if [ "$count" -gt 1 ]; then
            bulk_json="${bulk_json},"
        fi

        bulk_json="${bulk_json}{\"projectName\":\"${APPKEY}\",\"projectVersion\":\"1.0.0\",\"logVersion\":\"v2\",\"body\":${escaped_line},\"logSource\":\"nginx\",\"logType\":\"${log_type}\",\"host\":\"${HOSTNAME}\",\"lncBulkIndex\":${index}}"

        # BULK_SIZE에 도달하면 전송
        if [ "$count" -ge "$BULK_SIZE" ]; then
            bulk_json="${bulk_json}]"
            curl -s -X POST "$API_URL" \
                -H "Content-Type: application/json" \
                -d "$bulk_json" > /dev/null 2>&1
            bulk_json="["
            count=0
        fi
    done <<< "$lines"

    # 남은 로그 전송
    if [ "$count" -gt 0 ]; then
        bulk_json="${bulk_json}]"
        curl -s -X POST "$API_URL" \
            -H "Content-Type: application/json" \
            -d "$bulk_json" > /dev/null 2>&1
    fi

    echo "$total_lines"
}

# === 실행 ===
NEW_ACCESS_POS=$(send_logs "$ACCESS_LOG" "nginx-access" "$ACCESS_POS")
NEW_ERROR_POS=$(send_logs "$ERROR_LOG" "nginx-error" "$ERROR_POS")

# === 상태 저장 ===
echo "{\"access_lines\":${NEW_ACCESS_POS},\"error_lines\":${NEW_ERROR_POS}}" > "$STATE_FILE"
