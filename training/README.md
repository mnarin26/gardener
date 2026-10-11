# Outdoor navigation fine-tune (YOLO segmentation)

Scaffold for **fine-tuning** a pretrained Ultralytics YOLO segmentation model on outdoor surface classes (road / sidewalk / wall). Training is meant to run on the user’s Windows PC (RTX 3060), not in this cloud environment.

Android app build is unchanged: this folder is not part of the Gradle project.

## Why Option A (Ultralytics + Roboflow)

| Path | Fit for Windows + 3060 non-expert |
|------|-----------------------------------|
| **A — YOLO-seg + Roboflow** (chosen) | Free Roboflow account, one-click YOLO export, `yolov8n-seg.pt` fine-tune, CUDA via pip wheels |
| B — DeepLab on official Cityscapes | Better classic semantic labels, but **account + license signup** and heavier PyTorch training stack |

Cityscapes **does** have `road` / `sidewalk` / `wall` / `fence`. It does **not** have a dedicated **grate / street drain** class. Curb is also not a separate Cityscapes-19 label (edge ≈ sidewalk↔road boundary).

## Phase plan

1. **Phase 1 (now):** Fine-tune on Roboflow Cityscapes-style set  
   [`luigiaworkspace/cityscapes-zz0ur`](https://universe.roboflow.com/luigiaworkspace/cityscapes-zz0ur) (v10) — includes `road`, `sidewalk`, `wall`, `fence`, …  
   - Lighter alternative (road+sidewalk only): [`yolo-s6mwf/sidewalk-6imhx`](https://universe.roboflow.com/yolo-s6mwf/sidewalk-6imhx)
2. **Phase 2:** Add **grates** (and sharper **curb edge**) by labeling short phone videos. Closest public labels: BIAA `drainage_inlet` / `curb_edge` on [Universe](https://universe.roboflow.com/biaa/biaa_v100_gt) (small; no ready YOLO version may exist — generate export yourself).

**License note:** Official Cityscapes requires their terms. Prefer research/personal use; verify Roboflow project + Cityscapes license before commercial redistribution.

## Quick start (Windows PowerShell)

```powershell
cd training
python -m venv .venv
.\.venv\Scripts\Activate.ps1

# CUDA torch for RTX 3060 (install BEFORE ultralytics pulls a CPU wheel)
pip install torch torchvision --index-url https://download.pytorch.org/whl/cu124
pip install -r requirements.txt
python -c "import torch; print(torch.cuda.is_available(), torch.cuda.get_device_name(0) if torch.cuda.is_available() else 'cpu')"

$env:ROBOFLOW_API_KEY="YOUR_KEY"
python scripts/download_roboflow.py

# data.yaml path is printed by the download script — adjust if folder name differs
python scripts/train_yolo_seg.py --data datasets/Cityscapes-10/data.yaml --epochs 50 --batch 8 --device 0

python scripts/check_results.py --run runs/outdoor_nav_seg
```

*(Ultralytics 8.4+ writes to `runs/<name>/`. Older versions used `runs/segment/<name>/` — pass the folder that actually contains `weights/best.pt`.)*

Optional smoke (2 epochs, synthetic masks — verifies install only):

```powershell
python scripts/smoke_test.py
```

## Scripts

| Script | Role |
|--------|------|
| `scripts/download_roboflow.py` | Download Universe dataset as YOLOv8 |
| `scripts/train_yolo_seg.py` | Fine-tune pretrained `yolov8n-seg.pt` |
| `scripts/check_results.py` | List weights/plots; optional single-image predict |
| `scripts/smoke_test.py` | Tiny local train (no Roboflow) |

## Phone export

Next phase: export `best.pt` → ONNX / TFLite (and optionally Qualcomm AI Hub). Not covered by these scripts yet.

## Class mapping (Turkish → model)

See the user guide in the project store: `docs/ince-ayar-duvar-kaldirim.md`.
