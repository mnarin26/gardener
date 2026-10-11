# Gardener

Otonom çim biçme robotu — **robot uygulaması iskeleti** (Kotlin + Jetpack Compose).

- **Paket / applicationId:** `com.mustafa.gardener` *(stated assumption)*
- **Min SDK:** 26
- **Target / compile SDK:** 37
- **Mod:** Runtime modules SIMULATION; camera preview screen can open REAL CameraX (~640×480 target). Vision: pluggable `VisionEngine` — SIM / unloaded / failed, plus bundled **YOLOv8n COCO TFLite** detection trial (boxes + labels; default). FFNet Cityscapes seg remains optional behind a flag. LiDAR / ESP32 yok.
- **Cihaz kamera / detect / NPU:** henüz doğrulanmadı — S24 Ultra testi gerekir. Ultralytics ağırlıkları **AGPL-3.0**.

Merhaba demo kaldırıldı; biçme otonomisi iddiası yoktur. YOLOv8n smoke denemesi debug APK’dadır; PC fine-tune ayrı.

## Derleme

```bash
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Eğitim (PC — Android derlemesini etkilemez)

İnce ayar iskeleti: [`training/`](training/) (Ultralytics YOLO segmentasyon + Roboflow veri). Gradle yalnızca `:app` içerir; `training/` telefona dahil değildir.

## Modüller

CameraManager, VisionManager, ObjectManager, LiDARManager, ServoController, GNSSManager, IMUManager, EncoderManager, LandmarkManager, SensorFusion, Localization, MapManager, PathPlanner, ObstacleAvoidance, MissionManager, Esp32Communication, CalibrationManager, SimulationManager — hepsi ortak arayüz + `Sim*` implementasyon.
