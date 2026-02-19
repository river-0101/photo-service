#\!/bin/bash
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
        if curl -s --connect-timeout 2 --max-time 3 http://${IP}:8080/actuator/health > /dev/null 2>&1; then
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

# Write valid JSON (keep previous targets if scan finds nothing)
if [ -n "$ENTRIES" ]; then
    echo "[${ENTRIES}]" | python3 -m json.tool > ${TARGETS_FILE}.tmp 2>/dev/null
    if [ $? -eq 0 ]; then
        mv ${TARGETS_FILE}.tmp $TARGETS_FILE
    fi
fi
