#!/usr/bin/env python3
import json
import os
import subprocess
import sys
import time
from pathlib import Path
from tempfile import TemporaryDirectory


SUPPORTED_IMAGE_SUFFIXES = {
    ".bmp",
    ".dib",
    ".jpeg",
    ".jpg",
    ".png",
    ".webp",
    ".pbm",
    ".pgm",
    ".ppm",
    ".pnm",
    ".sr",
    ".ras",
    ".tiff",
    ".tif",
    ".pdf",
}
DEFAULT_MAX_SIDE = 1216


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: paddle_ocr.py <image-path>", file=sys.stderr)
        return 2

    image_path = Path(sys.argv[1])
    if not image_path.is_file():
        print(f"image not found: {image_path}", file=sys.stderr)
        return 2

    started_at = time.time()
    language = os.environ.get("BREWLOG_PADDLE_OCR_LANG", "korean")
    detection_model = os.environ.get("BREWLOG_PADDLE_OCR_DETECTION_MODEL", "PP-OCRv5_server_det")
    recognition_model = os.environ.get("BREWLOG_PADDLE_OCR_RECOGNITION_MODEL", "korean_PP-OCRv5_mobile_rec")
    max_side = int(os.environ.get(
        "BREWLOG_PADDLE_OCR_MAX_INPUT_SIDE",
        os.environ.get("BREWLOG_PADDLE_OCR_MAX_SIDE", str(DEFAULT_MAX_SIDE)),
    ))
    try:
        with prepare_input_image(image_path, max_side) as prepared_path:
            try:
                from paddleocr import PaddleOCR
            except Exception as exc:
                print(f"failed to import paddleocr: {type(exc).__name__}: {exc}", file=sys.stderr)
                return 3

            ocr = PaddleOCR(
                lang=language,
                text_detection_model_name=detection_model,
                text_recognition_model_name=recognition_model,
                use_doc_orientation_classify=False,
                use_doc_unwarping=False,
                use_textline_orientation=False,
            )
            result = ocr.predict(str(prepared_path))
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
        "engine": "paddleocr",
        "language": language,
        "detectionModel": detection_model,
        "recognitionModel": recognition_model,
        "seconds": round(time.time() - started_at, 3),
    }, ensure_ascii=False))
    return 0


class PreparedInputImage:
    def __init__(self, original_path: Path, max_side: int):
        self.original_path = original_path
        self.max_side = max_side
        self.temp_directory = None
        self.path = original_path

    def __enter__(self) -> Path:
        suffix = self.original_path.suffix.lower()
        if suffix not in SUPPORTED_IMAGE_SUFFIXES:
            raise ValueError(
                "unsupported input file type. HEIC/HEIF files must be converted by the Java image preprocessor first. "
                "supported suffixes: " + ", ".join(sorted(SUPPORTED_IMAGE_SUFFIXES))
            )
        if not self.needs_resize():
            return self.path

        self.temp_directory = TemporaryDirectory()
        converted_path = Path(self.temp_directory.name) / f"{self.original_path.stem}.png"
        self.run_sips(["-s", "format", "png", "-Z", str(self.max_side), str(self.original_path), "--out", str(converted_path)])
        self.path = converted_path
        return self.path

    def __exit__(self, exc_type, exc, tb) -> None:
        if self.temp_directory is not None:
            self.temp_directory.cleanup()

    def needs_resize(self) -> bool:
        if self.max_side <= 0:
            return False
        try:
            metadata = subprocess.run(
                ["sips", "-g", "pixelWidth", "-g", "pixelHeight", str(self.original_path)],
                check=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            ).stdout
        except Exception:
            return False

        width = parse_sips_dimension(metadata, "pixelWidth")
        height = parse_sips_dimension(metadata, "pixelHeight")
        return max(width, height) > self.max_side

    def run_sips(self, args: list[str]) -> None:
        try:
            subprocess.run(
                ["sips", *args],
                check=True,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.PIPE,
                text=True,
            )
        except subprocess.CalledProcessError as exc:
            raise ValueError(f"failed to prepare image with sips: {exc.stderr.strip()}") from exc


def parse_sips_dimension(metadata: str, key: str) -> int:
    for line in metadata.splitlines():
        if key in line:
            _, _, value = line.partition(":")
            try:
                return int(value.strip())
            except ValueError:
                return 0
    return 0


def prepare_input_image(image_path: Path, max_side: int) -> PreparedInputImage:
    return PreparedInputImage(image_path, max_side)


if __name__ == "__main__":
    raise SystemExit(main())
