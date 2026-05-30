import json
import os
from pathlib import Path
import sys
from contextlib import redirect_stdout

cache_dir = Path(__file__).resolve().parents[1] / "paddleocr_cache"
cache_dir.mkdir(parents=True, exist_ok=True)
os.environ.setdefault("PADDLE_HOME", str(cache_dir))
os.environ.setdefault("PADDLEX_HOME", str(cache_dir))
os.environ.setdefault("PADDLEX_CACHE_DIR", str(cache_dir))
os.environ.setdefault("PADDLE_PDX_CACHE_HOME", str(cache_dir))
os.environ.setdefault("FLAGS_use_mkldnn", "0")
os.environ.setdefault("FLAGS_enable_mkldnn", "0")
os.environ.setdefault("PADDLE_PDX_ENABLE_MKLDNN_BYDEFAULT", "False")


def collect_texts(value, lines):
    if value is None:
        return
    if isinstance(value, dict):
        for key, child in value.items():
            if key in {"rec_text", "text"} and isinstance(child, str):
                lines.append(child)
            elif key == "rec_texts" and isinstance(child, list):
                lines.extend(str(item) for item in child if item)
            else:
                collect_texts(child, lines)
        return
    if isinstance(value, (list, tuple)):
        if len(value) >= 2 and isinstance(value[1], (list, tuple)) and value[1]:
            text = value[1][0]
            if isinstance(text, str):
                lines.append(text)
                return
        for child in value:
            collect_texts(child, lines)
        return
    json_value = getattr(value, "json", None)
    if json_value is not None:
        collect_texts(json_value, lines)


def flatten_lines(result):
    lines = []
    collect_texts(result, lines)
    return lines


def create_ocr():
    with redirect_stdout(sys.stderr):
        from paddleocr import PaddleOCR

        try:
            return PaddleOCR(
                lang="en",
                text_detection_model_name="PP-OCRv5_mobile_det",
                text_recognition_model_name="en_PP-OCRv5_mobile_rec",
                use_doc_orientation_classify=False,
                use_doc_unwarping=False,
                use_textline_orientation=False,
            )
        except TypeError:
            return PaddleOCR(use_angle_cls=True, lang="en", show_log=False)


def read_image(ocr, image_path):
    with redirect_stdout(sys.stderr):
        if hasattr(ocr, "predict"):
            return ocr.predict(image_path)
        return ocr.ocr(image_path, cls=True)


def main():
    if len(sys.argv) != 3:
        print(json.dumps({"error": "Expected front and back image paths"}))
        return 2

    ocr = create_ocr()
    all_lines = []
    for image_path in sys.argv[1:]:
        all_lines.extend(flatten_lines(read_image(ocr, image_path)))

    print(json.dumps({"text": "\n".join(all_lines), "lines": all_lines}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
