#!/bin/bash
TARGETS_FILE=/home/rocky/monitoring/targets/was_targets.json
LB_IP="10.0.20.5"
SERVICE_GW="10.0.20.77"
TEMP_DIR=$(mktemp -d)
MAX_PARALLEL=20

# Batch parallel scan - limit concurrency to avoid resource exhaustion
count=0
for i in $(seq 1 254); do
    IP="10.0.20.$i"
    if [ "$IP" = "$LB_IP" ] || [ "$IP" = "$SERVICE_GW" ]; then
        continue
    fi
    (
        if curl -s --connect-timeout 4 --max-time 6 http://${IP}:8080/actuator/health > /dev/null 2>&1; then
            echo "${IP}" > ${TEMP_DIR}/${i}
        fi
    ) &
    count=$((count + 1))
    if [ $count -ge $MAX_PARALLEL ]; then
        wait
        count=0
    fi
done
wait

# Build JSON from results
ENTRIES=""
for f in $(ls ${TEMP_DIR}/ 2>/dev/null | sort -n); do
    IP=$(cat ${TEMP_DIR}/${f})
    if [ -n "$ENTRIES" ]; then
        ENTRIES="${ENTRIES},"
    fi
    ENTRIES="${ENTRIES}{\"targets\":[\"${IP}:8080\"],\"labels\":{\"instance_ip\":\"${IP}\",\"service\":\"photo-service\",\"tier\":\"was\"}}"
done

rm -rf ${TEMP_DIR}

# Merge with previous targets - don't remove targets found in the last scan
# This prevents momentary health check failures from dropping known instances
if [ -n "$ENTRIES" ] && [ -f "$TARGETS_FILE" ]; then
    PREV_IPS=$(python3 -c "import json; data=json.load(open('${TARGETS_FILE}')); [print(e['labels']['instance_ip']) for e in data]" 2>/dev/null)
    for PREV_IP in $PREV_IPS; do
        if ! echo "$ENTRIES" | grep -q "$PREV_IP"; then
            # Previous target not found in this scan - keep it (will be removed if missed again next scan)
            MISS_FILE="/tmp/discover_miss_${PREV_IP}"
            if [ -f "$MISS_FILE" ]; then
                # Missed 2 consecutive scans - actually remove it
                rm -f "$MISS_FILE"
            else
                # First miss - keep the target, mark as missed
                touch "$MISS_FILE"
                ENTRIES="${ENTRIES},{\"targets\":[\"${PREV_IP}:8080\"],\"labels\":{\"instance_ip\":\"${PREV_IP}\",\"service\":\"photo-service\",\"tier\":\"was\"}}"
            fi
        else
            # Target found - clear any miss marker
            rm -f "/tmp/discover_miss_${PREV_IP}" 2>/dev/null
        fi
    done
fi

# Write valid JSON (keep previous targets if scan finds nothing)
if [ -n "$ENTRIES" ]; then
    echo "[${ENTRIES}]" | python3 -m json.tool > ${TARGETS_FILE}.tmp 2>/dev/null
    if [ $? -eq 0 ]; then
        mv ${TARGETS_FILE}.tmp $TARGETS_FILE
    fi
fi
