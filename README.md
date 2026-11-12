# NextRAM Manager 2.0

Нативное приложение-компаньон для Magisk-модуля [NextRAM](https://github.com/LineXin/NextRAM).
Без WebView, без единой зависимости, интерфейс целиком на Java.

## Что умеет

- Живой индикатор занятой памяти (обновление каждые 2 секунды), кэш, swap и ZRAM из `/proc/meminfo` и `/proc/swaps`.
- Читает и правит `/data/adb/modules/NextRAM/config.conf`: переключатели, слайдеры, выбор алгоритма ZRAM.
- Кнопка «Применить» записывает конфиг и запускает `service.sh` модуля.
- Сброс кэша страниц (`drop_caches`) и просмотр логов модуля.
- Корректно работает без root: показывает память и честно пишет, что модуль недоступен.

## Сборка

### Вариант 1: GitHub Actions (APK без компьютера)

1. Загрузите папку проекта в репозиторий на GitHub (можно прямо с телефона через веб-интерфейс или приложение GitHub).
2. Откройте вкладку **Actions** → workflow **Build APK** → **Run workflow**.
3. Через 2–4 минуты в разделе артефактов появится `NextRAM-Manager-apk.zip` с `app-debug.apk`.

### Вариант 2: AndroidIDE на телефоне

Открыть папку проекта как Gradle-проект, дождаться синхронизации, `Build` → `Assemble debug`.

### Вариант 3: Android Studio

`File → Open` → выбрать папку проекта → Run.

## Структура

```
app/src/main/java/com/nextram/manager/
  MainActivity.java   — весь интерфейс и логика
  Gauge.java          — индикатор памяти на Canvas
  Shell.java          — запуск команд через su
```

## Требования к устройству

- Android 7.0+ (minSdk 24), root через Magisk или KernelSU.
- Модуль NextRAM, установленный в `/data/adb/modules/NextRAM`.

## Примечание

Пакет `com.nextram.manager` совпадает с текущим приложением в репозитории, поэтому новая версия
установится поверх старой. Если подпись не совпадёт — сначала удалите старое приложение.

Подпись здесь отладочная (`signingConfig signingConfigs.debug`), для публикации замените на свою.
