#!/usr/bin/env python3
"""Tiny smoke test: synthetic 2-class seg labels, 2 train steps. Verifies Ultralytics path.

Does NOT download Roboflow and is NOT a real outdoor-nav model.
"""

from __future__ import annotations

import sys
from pathlib import Path


def _write_tiny_dataset(root: Path) -> Path:
    import numpy as np

    try:
        import cv2
    except ImportError:
        print("ERROR: opencv required", file=sys.stderr)
        raise SystemExit(1)

    img_train = root / "images" / "train"
    img_val = root / "images" / "val"
    lbl_train = root / "labels" / "train"
    lbl_val = root / "labels" / "val"
    for d in (img_train, img_val, lbl_train, lbl_val):
        d.mkdir(parents=True, exist_ok=True)

    def make_sample(img_dir: Path, lbl_dir: Path, name: str, cls: int) -> None:
        h, w = 128, 128
        img = np.zeros((h, w, 3), dtype=np.uint8)
        img[:, :] = (40, 40, 40)
        # Filled rectangle as a fake mask region
        x1, y1, x2, y2 = 20, 40, 100, 90
        color = (80, 80, 200) if cls == 0 else (80, 200, 80)
        cv2.rectangle(img, (x1, y1), (x2, y2), color, -1)
        cv2.imwrite(str(img_dir / f"{name}.jpg"), img)
        # YOLO-seg polygon normalized
        poly = [
            x1 / w,
            y1 / h,
            x2 / w,
            y1 / h,
            x2 / w,
            y2 / h,
            x1 / w,
            y2 / h,
        ]
        line = f"{cls} " + " ".join(f"{v:.6f}" for v in poly) + "\n"
        (lbl_dir / f"{name}.txt").write_text(line, encoding="utf-8")

    make_sample(img_train, lbl_train, "t0", 0)
    make_sample(img_train, lbl_train, "t1", 1)
    make_sample(img_val, lbl_val, "v0", 0)
    make_sample(img_val, lbl_val, "v1", 1)

    data_yaml = root / "data.yaml"
    data_yaml.write_text(
        f"path: {root.as_posix()}\n"
        "train: images/train\n"
        "val: images/val\n"
        "names:\n"
        "  0: road\n"
        "  1: sidewalk\n",
        encoding="utf-8",
    )
    return data_yaml


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    smoke_dir = root / "datasets" / "_smoke"
    smoke_dir.mkdir(parents=True, exist_ok=True)
    data_yaml = _write_tiny_dataset(smoke_dir)

    try:
        from ultralytics import YOLO
    except ImportError:
        print("ERROR: pip install -r requirements.txt first", file=sys.stderr)
        return 1

    # Prefer GPU if present; fall back to CPU for CI/cloud smoke.
    import torch

    device = "0" if torch.cuda.is_available() else "cpu"
    print(f"Smoke train on device={device} (2 epochs, batch=2, imgsz=128)")
    model = YOLO("yolov8n-seg.pt")
    model.train(
        data=str(data_yaml),
        epochs=2,
        imgsz=128,
        batch=2,
        device=device,
        project=str(root / "runs"),
        name="smoke_seg",
        exist_ok=True,
        patience=2,
        plots=False,
        verbose=True,
    )
    best = root / "runs" / "segment" / "smoke_seg" / "weights" / "best.pt"
    if not best.is_file():
        # Ultralytics may nest under runs/segment or runs/smoke depending on version
        candidates = list((root / "runs").rglob("smoke_seg/**/best.pt"))
        if candidates:
            best = candidates[0]
    print(f"Smoke OK. weights: {best} exists={best.is_file()}")
    return 0 if best.is_file() else 1


if __name__ == "__main__":
    raise SystemExit(main())
