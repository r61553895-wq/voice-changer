# Voice Changer — Real-Time AI Voice Changer (Android MVP)

Рабочий MVP: реальный захват микрофона (Oboe/AAudio), обработка голоса в
нативном C++ (pitch / formant / robot / reverb / echo), Compose UI,
Clean Architecture + MVVM, и готовый слот для AI voice conversion
(ONNX Runtime Mobile / TensorFlow Lite).

## Структура проекта

```
app/
 ├── src/main/cpp/            # C++ движок: Oboe full-duplex, pitch/formant/effects DSP
 ├── src/main/java/.../presentation/   # Compose UI + ViewModel (MVVM)
 ├── src/main/java/.../domain/         # Use cases, модели, интерфейсы репозиториев
 ├── src/main/java/.../data/           # Реализации репозиториев
 ├── src/main/java/.../audio_engine/   # JNI-обёртка + foreground service
 ├── src/main/java/.../ai_model/       # ONNX / TFLite / PassThrough модели
 └── src/main/java/.../di/             # Hilt-модули
```

## 1. Как запустить проект

Тебе нужен **компьютер с Android Studio** (Windows/macOS/Linux) — билд с
NDK/CMake на телефоне не делается. Раз у тебя сейчас только телефон, вот
рабочие варианты:

### Вариант A (рекомендуется, если появится доступ к ПК/ноутбуку)
1. Установи Android Studio (Hedgehog+), SDK 34, NDK 26+, CMake.
2. Открой папку `VoiceChangerApp` как проект.
3. Studio сама подтянет Oboe через `FetchContent` в CMake (нужен интернет
   при первой сборке).
4. Подключи телефон по USB (Developer Options → USB debugging) → Run ▶.

### Вариант B — собрать APK "с телефона" без своего ПК
На голом телефоне полноценно собрать C++/NDK проект нельзя (Termux не
тянет полный Android SDK+NDK стабильно), но есть 3 рабочих пути:

1. **GitHub Actions (бесплатно, только телефон + браузер)**
   - Залей эту папку как репозиторий на GitHub (можно через приложение
     GitHub или через Working Copy/Termux + git).
   - Добавь workflow `.github/workflows/build.yml`:
     ```yaml
     name: Build APK
     on: [push]
     jobs:
       build:
         runs-on: ubuntu-latest
         steps:
           - uses: actions/checkout@v4
           - uses: actions/setup-java@v4
             with: { distribution: 'temurin', java-version: '17' }
           - uses: android-actions/setup-android@v3
           - run: yes | sdkmanager --install "ndk;26.1.10909125" "cmake;3.22.1"
           - run: ./gradlew assembleDebug
           - uses: actions/upload-artifact@v4
             with: { name: app-debug, path: app/build/outputs/apk/debug/*.apk }
     ```
   - GitHub соберёт APK в облаке, ты скачаешь его прямо в браузере телефона
     и установишь (разреши "Install unknown apps").

2. **Облачная IDE с телефона**: Gitpod / GitHub Codespaces / Replit —
   открываешь репозиторий в браузере телефона, там полноценный Linux с
   терминалом, ставишь `sdkmanager`, гоняешь `./gradlew assembleDebug`,
   скачиваешь APK.

3. Попроси знакомого с ПК собрать один раз — дальше обновлять код и
   пересобирать через GitHub Actions уже можно только с телефона.

### Тестовый режим без AI-модели
По умолчанию `PassThroughModel` активен — приложение полностью работает
(pitch/formant/robot/reverb/echo) без единого файла модели. Переключатель
"AI Voice Conversion" на экране включает попытку загрузить ONNX-модель;
если файла нет — автоматически остаётся DSP-режим.

## 2. Где подключить AI-модель

1. Экспортируй модель (например, RVC checkpoint) в ONNX:
   `torch.onnx.export(model, dummy_input, "model.onnx", input_names=["audio_in"], output_names=["audio_out"])`
2. Положи файл в `app/src/main/assets/models/onnx_rvc.onnx`.
3. Проверь имена тензоров в `OnnxVoiceConversionModel.kt` (`"audio_in"` /
   `"audio_out"`) — поправь под свой граф.
4. Включи тумблер "AI Voice Conversion" в приложении — `LoadVoiceModelUseCase`
   вызовет `VoiceModelRepository.loadModel()`, а нативный `AudioEngine`
   пропустит DSP-выход через `mAiCallback` (см. `audio_engine.cpp`,
   `setAiCallback`) перед выводом в динамик.
5. Для TFLite — аналогично, файл `models/tflite_vc.tflite`, реализация в
   `TFLiteVoiceConversionModel.kt`.
6. Чтобы добавить третий рантайм (например, ExecuTorch) — допиши case в
   `ai_model/ModelFactory.kt` и реализуй `VoiceConversionModel` — больше
   нигде код трогать не нужно (это и есть точка расширения архитектуры).

## 3. Как улучшить качество голоса

- **Formant shifter**: сейчас блок-синхронный STFT (упрощение для MVP).
  Замени на honest hop-based overlap-add (hop = fftSize/4) с отдельным
  кольцевым буфером — уберёт остаточные щелчки на резких пресетах
  (Monster/Anime).
- **Pitch shifter**: текущий granular OLA — быстрый и малозадержечный,
  но на больших сдвигах (>1 октавы) слышны артефакты. Для студийного
  качества добавь PSOLA с детекцией periods (autocorrelation pitch
  tracking) или WSOLA.
- **AI-модель вместо чистого DSP** — реальный neural vocoder (RVC/so-vits)
  даёт наиболее натуральный тембр, но требует буферизации 200-400ms для
  контекста — это компромисс между качеством и задержкой; можно сделать
  гибрид: DSP для realtime-режима звонков, AI-режим для записи с чуть
  большей задержкой.
- Добавь noise suppression (WebRTC AudioProcessing / RNNoise) перед
  pitch-shifter — уменьшит шум, который AI/DSP иначе тоже трансформируют.
- AEC (echo cancellation), если голос идёт в звонок, а не просто в
  наушники — иначе будет петля.

## 4. Как сделать системный виртуальный микрофон в будущем

Чтобor другие приложения (Zoom, Discord, Telegram) слышали уже изменённый
голос, нужен virtual audio input, доступный системе как отдельное
устройство записи:

- **Android не даёт третьим приложениям создавать системный virtual mic**
  без root — это ограничение платформы (в отличие от desktop, где есть
  VB-Cable/BlackHole).
- Реалистичные пути:
  1. **AudioPlaybackCapture API (Android 10+)** — можно перехватывать
     аудио, воспроизводимое другими приложениями, но не наоборот
     (подменить их источник записи) без root.
  2. **Root + Xposed/LSPosed module**, который хукает `AudioRecord`/
     `AudioFlinger` и подменяет источник на буфер твоего приложения —
     рабочий, но требует root и ломается между прошивками.
  3. **HAL-уровень (кастомная прошивка/AOSP)**: реализовать `audio.primary`
     virtual input device в audio HAL — тяжёлый путь, годится только для
     кастомных ROM или встраиваемых решений.
  4. **Практичный компромисс уже сегодня**: интеграция как in-call effect
     через `AudioEffect`/`VoiceInteractionService` для конкретных
     совместимых приложений, либо запуск изменённого голоса в
     Bluetooth-гарнитуру через `AudioTrack` + `USAGE_VOICE_COMMUNICATION`,
     что для многих сценариев (стрим, запись, локальные звонки через
     собственный SIP-клиент) уже достаточно без root.
- Если нужен voice changer именно "внутрь" Discord/Zoom — сейчас это
  реалистично только через root-хуки или собственный SIP/WebRTC клиент,
  где ты сам контролируешь источник аудио.

## 5. Если у тебя только телефон (кратко)

- Сборка возможна **только** через облако: GitHub Actions / Codespaces /
  Gitpod (см. Вариант B выше) — это единственный практичный путь без ПК.
- Установи GitHub-приложение или Termux+git на телефон, чтобы запушить
  этот код в репозиторий.
- Готовый `.apk` из Actions ты скачаешь и установишь прямо в браузере
  телефона (Chrome → разрешить "Install unknown apps" для источника).
- Дальше весь цикл разработки (правка Kotlin/C++ файлов, коммит, билд)
  можно вести полностью с телефона через мобильный GitHub/Codespaces
  клиент — ПК не понадобится вообще.
