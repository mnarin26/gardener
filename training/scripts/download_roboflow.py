#!/usr/bin/env python3
"""Download a Roboflow Universe dataset in YOLOv8 segmentation format.

Requires ROBOFLOW_API_KEY in the environment (free account at roboflow.com).

Default project: Cityscapes-style instance seg with road / sidewalk / wall
(luigiaworkspace/cityscapes-zz0ur). Override with CLI flags for alternatives.
"""

from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser(description="Download Roboflow dataset (YOLOv8 format)")
    parser.add_argument(
        "--workspace",
        default="luigiaworkspace",
        help="Roboflow workspace slug",
    )
    parser.add_argument(
        "--project",
        default="cityscapes-zz0ur",
        help="Roboflow project slug",
    )
    parser.add_argument(
        "--version",
        type=int,
        default=10,
        help="Dataset version number",
    )
    parser.add_argument(
        "--format",
        default="yolov8",
        help="Export format (yolov8 for Ultralytics seg)",
    )
    parser.add_argument(
        "--out",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "datasets",
        help="Parent directory for the download",
    )
    args = parser.parse_args()

    api_key = os.environ.get("ROBOFLOW_API_KEY", "").strip()
    if not api_key:
        print(
            "ERROR: Set ROBOFLOW_API_KEY first.\n"
            "  1) Create a free account at https://roboflow.com\n"
            "  2) Copy your API key from Account Settings\n"
            "  3) PowerShell:  $env:ROBOFLOW_API_KEY=\"YOUR_KEY\"\n"
            "     CMD:         set ROBOFLOW_API_KEY=YOUR_KEY",
            file=sys.stderr,
        )
        return 1

    try:
        from roboflow import Roboflow
    except ImportError:
        print("ERROR: install deps first: pip install -r requirements.txt", file=sys.stderr)
        return 1

    args.out.mkdir(parents=True, exist_ok=True)
    os.chdir(args.out)

    print(f"Downloading {args.workspace}/{args.project} v{args.version} as {args.format} …")
    rf = Roboflow(api_key=api_key)
    project = rf.workspace(args.workspace).project(args.project)
    version = project.version(args.version)
    dataset = version.download(args.format)
    print(f"Done. Location: {dataset.location}")
    print("Next: point train_yolo_seg.py at that folder's data.yaml")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
