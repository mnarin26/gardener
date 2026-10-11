# Gardener

Otonom çim biçme robotu — **robot uygulaması iskeleti** (Kotlin + Jetpack Compose).

- **Paket / applicationId:** `com.mustafa.gardener` *(stated assumption)*
- **Min SDK:** 26
- **Target / compile SDK:** 37
- **Mod:** SIMULATION — gerçek kamera / LiDAR / ESP32 / YOLO yok

Merhaba demo kaldırıldı; bu iskelet mimari ve simülasyon sürücüleri içindir, çalışan biçme iddiası yoktur.

## Derleme

```bash
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Modüller

CameraManager, VisionManager, ObjectManager, LiDARManager, ServoController, GNSSManager, IMUManager, EncoderManager, LandmarkManager, SensorFusion, Localization, MapManager, PathPlanner, ObstacleAvoidance, MissionManager, Esp32Communication, CalibrationManager, SimulationManager — hepsi ortak arayüz + `Sim*` implementasyon.
