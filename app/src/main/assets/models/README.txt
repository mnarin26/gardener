Gardener — model asset placeholder
====================================

Put an exported on-device model here later, e.g.:

  outdoor_nav_seg.tflite

Expected asset path (see VisionModelPaths.ASSET_MODEL_RELATIVE):

  assets/models/outdoor_nav_seg.tflite

Alternative runtime copy location:

  <app filesDir>/models/outdoor_nav_seg.tflite

Phase 1 class labels: wall, sidewalk, road, fence
Phase 2 (later): grate

No model file is shipped in this scaffold. Until a TFLite / LiteRT /
Ultralytics-Android runtime is wired, VisionManager reports unloaded /
not configured and does NOT run inference.
