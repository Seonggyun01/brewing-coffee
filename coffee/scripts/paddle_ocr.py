#!/usr/bin/env python3
import json
import sys
import time
from pathlib import Path


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: paddle_ocr.py <image-path>", file=sys.stderr)
        return 2

    image_path = Path(sys.argv[1])
    if not image_path.is_file():
        print(f"image not found: {image_path}", file=sys.stderr)
        return 2

    try:
        from paddleocr import PaddleOCR
    except Exception as exc:
        print(f"failed to import paddleocr: {type(exc).__name__}: {exc}", file=sys.stderr)
        return 3

    started_at = time.time()
    try:
        ocr = PaddleOCR(
            lang="korean",
            use_doc_orientation_classify=False,
            use_doc_unwarping=False,
            use_textline_orientation=False,
        )
        result = ocr.predict(str(image_path))
    except Exception as exc:
        print(f"paddleocr failed: {type(exc).__name__}: {exc}", file=sys.stderr)
        return 4

    items = []
    for page in result:
        json_attr = getattr(page, "json", None)
        if callable(json_attr):
            page_json = json_attr()
        elif isinstance(json_attr, dict):
            page_json = json_attr
        else:
            page_json = {}
        rec_texts = page_json.get("rec_texts") or page_json.get("res", {}).get("rec_texts") or []
        rec_scores = page_json.get("rec_scores") or page_json.get("res", {}).get("rec_scores") or []
        for index, text in enumerate(rec_texts):
            confidence = rec_scores[index] if index < len(rec_scores) else None
            items.append({"text": str(text), "confidence": confidence})

    print(json.dumps({
        "text": "\n".join(item["text"] for item in items),
        "items": items,
        "seconds": round(time.time() - started_at, 3),
    }, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
