Gardener — on-device vision models
==================================

Default smoke trial (object detection):

  yolov8n_float32.tflite          (~12.2 MB, Ultralytics YOLOv8n COCO float32)
  yolov8n_coco_labels.txt         (80 COCO class names)

Asset path used by VisionModelPaths.ASSET_YOLO_RELATIVE:

  assets/models/yolov8n_float32.tflite

Optional filesDir override:

  <app filesDir>/models/yolov8n_detect.tflite

Bundled but NOT default (FFNet Cityscapes seg — VisionBackend.FFNET_SEG):

  ffnet_122ns_lowres_w8a8.tflite   (~32 MB, Qualcomm AI Hub public asset)
  ffnet_122ns_lowres_labels.txt
  ffnet_122ns_lowres_metadata.json

See docs/yolov8n-trial.md (AGPL note for Ultralytics weights).
