Gardener — on-device segmentation models
========================================

Bundled ready-seg trial (Cityscapes):

  ffnet_122ns_lowres_w8a8.tflite   (~32 MB, Qualcomm AI Hub public asset)
  ffnet_122ns_lowres_labels.txt
  ffnet_122ns_lowres_metadata.json

Asset path used by VisionModelPaths.ASSET_MODEL_RELATIVE:

  assets/models/ffnet_122ns_lowres_w8a8.tflite

Optional fine-tune drop-in (filesDir):

  <app filesDir>/models/outdoor_nav_seg.tflite

Legacy name still recognized if present:

  assets/models/outdoor_nav_seg.tflite

Highlight classes for overlay: road, sidewalk, wall, fence
(Cityscapes train ids; see ready-seg-trial.md).
