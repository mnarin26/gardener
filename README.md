# Gardener

Otonom çim biçme robotu — **robot uygulaması iskeleti** (Kotlin + Jetpack Compose).

- **Paket / applicationId:** `com.mustafa.gardener` *(stated assumption)*
- **Min SDK:** 26
- **Target / compile SDK:** 37
- **Mod:** Runtime modules SIMULATION; camera preview screen can open REAL CameraX (~640×480 target). LiDAR / ESP32 / YOLO yok.
- **Cihaz kamera:** henüz doğrulanmadı — telefon testi gerekir.

Merhaba demo kaldırıldı; bu iskelet mimari + CameraX önizleme içindir, çalışan biçme iddiası yoktur.

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
