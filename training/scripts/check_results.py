#!/usr/bin/env python3
"""Inspect a finished Ultralytics segment run and optionally predict on one image."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser(description="Check YOLO-seg fine-tune results")
    parser.add_argument(
        "--run",
        type=Path,
        required=True,
        help="Run folder, e.g. training/runs/segment/outdoor_nav_seg",
    )
    parser.add_argument(
        "--image",
        type=Path,
        default=None,
        help="Optional image for a quick overlay prediction",
    )
    parser.add_argument("--conf", type=float, default=0.25)
    args = parser.parse_args()

    run = args.run
    if not run.is_dir():
        print(f"ERROR: run folder not found: {run}", file=sys.stderr)
        return 1

    best = run / "weights" / "best.pt"
    last = run / "weights" / "last.pt"
    results_csv = run / "results.csv"
    args_yaml = run / "args.yaml"

    print("=== Run folder ===")
    print(run.resolve())
    for label, path in [("best.pt", best), ("last.pt", last), ("results.csv", results_csv), ("args.yaml", args_yaml)]:
        status = "OK" if path.is_file() else "MISSING"
        print(f"  [{status}] {label}: {path}")

    # Common plot artifacts Ultralytics writes
    for name in (
        "results.png",
        "confusion_matrix.png",
        "MaskP_curve.png",
        "MaskR_curve.png",
        "val_batch0_pred.jpg",
    ):
        p = run / name
        if p.is_file():
            print(f"  [OK] plot: {p.name}")

    if results_csv.is_file():
        try:
            lines = results_csv.read_text(encoding="utf-8").strip().splitlines()
            print("\n=== results.csv (last 3 rows) ===")
            for line in lines[:1] + lines[-3:]:
                print(line)
        except OSError as exc:
            print(f"Could not read results.csv: {exc}", file=sys.stderr)

    if args.image is not None:
        if not best.is_file():
            print("ERROR: best.pt missing; cannot predict", file=sys.stderr)
            return 1
        if not args.image.is_file():
            print(f"ERROR: image not found: {args.image}", file=sys.stderr)
            return 1
        try:
            from ultralytics import YOLO
        except ImportError:
            print("ERROR: ultralytics not installed", file=sys.stderr)
            return 1
        model = YOLO(str(best))
        out_dir = run / "check_predict"
        model.predict(
            source=str(args.image),
            conf=args.conf,
            save=True,
            project=str(out_dir.parent),
            name=out_dir.name,
            exist_ok=True,
        )
        print(f"\nPrediction saved under: {out_dir.resolve()}")

    print(
        "\nHow to read success (rough):\n"
        "  - results.csv metrics for mask/box mAP should trend up across epochs\n"
        "  - open val_batch*_pred.jpg — road/sidewalk/wall masks should look plausible\n"
        "  - if CUDA OOM during train, rerun with --batch 4 or --imgsz 512"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
