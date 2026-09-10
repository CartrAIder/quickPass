#!/usr/bin/env python3
"""파일명을 barcode로 해석해 QuickPass 상품 이미지 API를 호출한다."""

import argparse
import json
from pathlib import Path
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid


ALLOWED_TYPES = {".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".png": "image/png", ".webp": "image/webp"}


def multipart(file_path, content_type):
    boundary = "----quickpass-" + uuid.uuid4().hex
    header = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="image"; filename="{file_path.name}"\r\n'
        f"Content-Type: {content_type}\r\n\r\n"
    ).encode()
    return boundary, header + file_path.read_bytes() + f"\r\n--{boundary}--\r\n".encode()


def upload(base_url, token, file_path, replace):
    content_type = ALLOWED_TYPES[file_path.suffix.lower()]
    boundary, body = multipart(file_path, content_type)
    barcode = urllib.parse.quote(file_path.stem, safe="")
    url = f"{base_url.rstrip('/')}/api/admin/products/{barcode}/image?replace={str(replace).lower()}"
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": f"multipart/form-data; boundary={boundary}",
    }
    with urllib.request.urlopen(urllib.request.Request(url, body, headers, method="POST"), timeout=60) as response:
        return json.loads(response.read())


def main():
    parser = argparse.ArgumentParser(description="QuickPass 상품 이미지 일괄 등록")
    parser.add_argument(
        "image_directory",
        nargs="?",
        default=str(Path(__file__).with_name("images")),
        help="이미지 디렉터리(기본값: tools/product-image-importer/images)",
    )
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--token", required=True, help="관리자 JWT access token")
    parser.add_argument("--replace", action="store_true", help="기존 이미지를 새 이미지로 교체")
    args = parser.parse_args()

    directory = Path(args.image_directory)
    if not directory.is_dir():
        print(f"이미지 디렉터리를 찾을 수 없습니다: {directory}", file=sys.stderr)
        return 2

    counts = {"uploaded": 0, "skipped": 0, "missing": 0, "failed": 0}
    files = sorted(path for path in directory.iterdir() if path.is_file())
    for file_path in files:
        if file_path.suffix.lower() not in ALLOWED_TYPES:
            counts["failed"] += 1
            print(f"FAILED file={file_path.name} reason=unsupported-extension")
            continue
        try:
            result = upload(args.base_url, args.token, file_path, args.replace)
            key = "uploaded" if result["uploaded"] else "skipped"
            counts[key] += 1
            print(f"{key.upper()} barcode={file_path.stem} file={file_path.name}")
        except urllib.error.HTTPError as error:
            response = error.read().decode("utf-8", errors="replace")
            key = "missing" if error.code == 404 and "PRODUCT_NOT_FOUND" in response else "failed"
            counts[key] += 1
            print(f"{key.upper()} barcode={file_path.stem} http={error.code} response={response}")
        except OSError as error:
            counts["failed"] += 1
            print(f"FAILED file={file_path.name} error={error}")

    print("SUMMARY " + " ".join(f"{key}={value}" for key, value in counts.items()))
    return 1 if counts["failed"] or counts["missing"] else 0


if __name__ == "__main__":
    sys.exit(main())
