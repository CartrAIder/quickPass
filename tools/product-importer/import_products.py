#!/usr/bin/env python3
"""CSV 상품 데이터를 QuickPass 관리자 API로 반복 가능하게 등록한다."""

import argparse
import csv
import json
import sys
import urllib.error
import urllib.request


def request(url, token, payload):
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    return urllib.request.urlopen(urllib.request.Request(url, body, headers, method="POST"), timeout=30)


def main():
    parser = argparse.ArgumentParser(description="QuickPass 상품 CSV 일괄 등록")
    parser.add_argument("csv_file")
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--token", required=True, help="관리자 JWT access token")
    args = parser.parse_args()

    counts = {"created": 0, "skipped": 0, "failed": 0}
    with open(args.csv_file, encoding="utf-8-sig", newline="") as source:
        for row_number, row in enumerate(csv.DictReader(source), start=2):
            barcode = (row.get("barcode") or "").strip()
            try:
                payload = {
                    "barcode": barcode,
                    "name": row["name"].strip(),
                    "price": int(row["price"]),
                    "category": row["category"].strip(),
                    "status": row["status"].strip(),
                }
                with request(f"{args.base_url.rstrip('/')}/api/admin/products", args.token, payload):
                    counts["created"] += 1
                    print(f"CREATED row={row_number} barcode={barcode}")
            except urllib.error.HTTPError as error:
                response = error.read().decode("utf-8", errors="replace")
                if error.code == 409 and "DUPLICATE_PRODUCT_BARCODE" in response:
                    counts["skipped"] += 1
                    print(f"SKIPPED row={row_number} barcode={barcode} reason=duplicate")
                else:
                    counts["failed"] += 1
                    print(f"FAILED row={row_number} barcode={barcode} http={error.code} response={response}")
            except (KeyError, ValueError, OSError) as error:
                counts["failed"] += 1
                print(f"FAILED row={row_number} barcode={barcode} error={error}")

    print("SUMMARY " + " ".join(f"{key}={value}" for key, value in counts.items()))
    return 1 if counts["failed"] else 0


if __name__ == "__main__":
    sys.exit(main())
