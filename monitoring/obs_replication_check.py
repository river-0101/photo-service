#!/usr/bin/env python3
"""
Object Storage 리전 간 복제 검증 스크립트

판교(KR1)와 평촌(KR2) Object Storage 버킷의 오브젝트 수를 비교하여
복제 이상 여부를 감지하고, 차이 발생 시 Dooray Webhook으로 알림을 전송한다.

사용법:
  - 환경변수 설정 후 실행: python3 obs_replication_check.py
  - cron 등록: */5 * * * * docker compose -f /root/monitoring/docker-compose.yml run --rm obs-checker
"""

import json
import os
import sys
import urllib.request
from datetime import datetime, timezone, timedelta

import boto3
from botocore.config import Config


def get_env(key, default=None):
    value = os.environ.get(key, default)
    if value is None:
        print(f"[ERROR] 환경변수 {key}가 설정되지 않았습니다.")
        sys.exit(1)
    return value


def count_objects(endpoint, region, access_key, secret_key, bucket_name):
    """S3 API로 버킷 내 오브젝트 수를 카운트한다."""
    s3 = boto3.client(
        "s3",
        endpoint_url=endpoint,
        region_name=region,
        aws_access_key_id=access_key,
        aws_secret_access_key=secret_key,
        config=Config(
            signature_version="s3v4",
            retries={"max_attempts": 3, "mode": "standard"},
            connect_timeout=10,
            read_timeout=30,
        ),
    )

    count = 0
    continuation_token = None

    while True:
        kwargs = {"Bucket": bucket_name, "MaxKeys": 1000}
        if continuation_token:
            kwargs["ContinuationToken"] = continuation_token

        response = s3.list_objects_v2(**kwargs)
        count += response.get("KeyCount", 0)

        if not response.get("IsTruncated", False):
            break
        continuation_token = response.get("NextContinuationToken")

    return count


def send_dooray_alert(webhook_url, title, message):
    """Dooray Incoming Webhook으로 알림을 전송한다."""
    kst = timezone(timedelta(hours=9))
    now = datetime.now(kst).strftime("%Y-%m-%d %H:%M:%S KST")

    payload = {
        "text": f"**[OBS 복제 알림] {title}**\n\n{message}\n\n발생 시각: {now}"
    }

    req = urllib.request.Request(
        webhook_url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            if resp.status == 200:
                print(f"[INFO] Dooray 알림 전송 성공")
            else:
                print(f"[WARN] Dooray 응답 코드: {resp.status}")
    except Exception as e:
        print(f"[ERROR] Dooray 알림 전송 실패: {e}")


def main():
    # 환경변수 로드
    primary_endpoint = get_env("OBS_PRIMARY_ENDPOINT")
    primary_region = get_env("OBS_PRIMARY_REGION", "KR1")
    primary_bucket = get_env("OBS_PRIMARY_BUCKET")

    dr_endpoint = get_env("OBS_DR_ENDPOINT")
    dr_region = get_env("OBS_DR_REGION", "KR2")
    dr_bucket = get_env("OBS_DR_BUCKET")

    access_key = get_env("OBS_ACCESS_KEY")
    secret_key = get_env("OBS_SECRET_KEY")

    dooray_webhook = get_env("DOORAY_WEBHOOK_URL")
    threshold = int(get_env("OBS_DIFF_THRESHOLD", "5"))

    kst = timezone(timedelta(hours=9))
    now = datetime.now(kst).strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{now}] OBS 복제 검증 시작")

    # 판교 오브젝트 수 조회
    try:
        primary_count = count_objects(
            primary_endpoint, primary_region, access_key, secret_key, primary_bucket
        )
        print(f"  판교({primary_region}): {primary_count}개")
    except Exception as e:
        msg = f"판교({primary_region}) 버킷 조회 실패: {e}"
        print(f"[ERROR] {msg}")
        send_dooray_alert(dooray_webhook, "판교 OBS 조회 실패", msg)
        sys.exit(1)

    # 평촌 오브젝트 수 조회
    try:
        dr_count = count_objects(
            dr_endpoint, dr_region, access_key, secret_key, dr_bucket
        )
        print(f"  평촌({dr_region}): {dr_count}개")
    except Exception as e:
        msg = f"평촌({dr_region}) 버킷 조회 실패: {e}"
        print(f"[ERROR] {msg}")
        send_dooray_alert(dooray_webhook, "평촌 OBS 조회 실패", msg)
        sys.exit(1)

    # 비교
    diff = abs(primary_count - dr_count)
    print(f"  차이: {diff}개 (허용 임계값: {threshold}개)")

    if diff > threshold:
        title = "복제 불일치 감지"
        message = (
            f"판교({primary_region}): {primary_count}개\n"
            f"평촌({dr_region}): {dr_count}개\n"
            f"차이: {diff}개 (임계값: {threshold}개 초과)\n\n"
            f"Object Storage 리전 간 복제가 정상적으로 이루어지지 않고 있을 수 있습니다.\n"
            f"NHN Cloud 콘솔에서 복제 설정을 확인하세요."
        )
        print(f"[ALERT] {title}")
        send_dooray_alert(dooray_webhook, title, message)
    else:
        print(f"[OK] 복제 정상 (차이 {diff}개, 임계값 {threshold}개 이내)")


if __name__ == "__main__":
    main()
