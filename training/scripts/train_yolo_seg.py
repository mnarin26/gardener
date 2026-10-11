#!/usr/bin/env python3
"""Fine-tune Ultralytics YOLO segmentation (pretrained) for outdoor navigation classes.

Does NOT train from scratch. Default weights: yolov8n-seg.pt (nano — fits RTX 3060).
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description="YOLO-seg fine-tune (pretrained)")
    parser.add_argument(
        "--data",
        type=Path,
        required=True,
        help="Path to data.yaml from Roboflow download (or configs/outdoor_nav.example.yaml)",
    )
    parser.add_argument(
        "--model",
        default="yolov8n-seg.pt",
        help="Pretrained Ultralytics seg checkpoint (n/s/m). Default: yolov8n-seg.pt",
    )
    parser.add_argument("--epochs", type=int, default=50)
    parser.add_argument("--imgsz", type=int, default=640)
    parser.add_argument("--batch", type=int, default=8, help="Lower if CUDA OOM (e.g. 4)")
    parser.add_argument("--device", default="0", help="GPU id, or 'cpu'")
    parser.add_argument("--name", default="outdoor_nav_seg", help="Run name under runs/segment/")
    parser.add_argument(
        "--project",
        type=Path,
        default=root / "runs",
        help="Ultralytics project dir (default: training/runs)",
    )
    parser.add_argument(
        "--patience",
        type=int,
        default=20,
        help="Early-stopping patience (epochs without val improvement)",
    )
    args = parser.parse_args()

    if not args.data.is_file():
        print(f"ERROR: data yaml not found: {args.data}", file=sys.stderr)
        return 1

    try:
        from ultralytics import YOLO
    except ImportError:
        print("ERROR: pip install -r requirements.txt (and CUDA torch first)", file=sys.stderr)
        return 1

    print(f"Fine-tuning {args.model} on {args.data}")
    model = YOLO(args.model)
    results = model.train(
        data=str(args.data.resolve()),
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        device=args.device,
        project=str(args.project.resolve()),
        name=args.name,
        patience=args.patience,
        exist_ok=True,
    )
    save_dir = Path(results.save_dir) if hasattr(results, "save_dir") else args.project / "segment" / args.name
    best = save_dir / "weights" / "best.pt"
    print(f"Training finished. Best weights (if produced): {best}")
    print("Check metrics with: python scripts/check_results.py --run", save_dir)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
