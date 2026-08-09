# ReArchLauncher

**Caelestia Shell + Hyprland → Android home screen launcher port**

The actual QML from `caelestia/shell` running on Android via Qt 6.
Not a rebuild from scratch — a real port.

## Architecture

```
Linux (original)                      Android (this repo)
═══════════════════                   ══════════════════════════════
Caelestia Shell QML         →         Same QML, unchanged
caelestia/shell/components/ →         caelestia/shell/components/   ✅
caelestia/shell/modules/    →         caelestia/shell/modules/      ✅
caelestia/shell/services/   →         caelestia/shell/services/     ✅ (patched)
caelestia/shell/plugin/     →         caelestia/shell/plugin/       ✅ (shims below)
                                      
Quickshell framework        →         android/platform/qml/Quickshell/
Quickshell.Hyprland         →         android/platform/qml/Quickshell/Hyprland/
                                      
HyprExtras (Hyprland IPC)   →         android-shims/.../hyprextras_android.cpp
HyprDevices (input devices) →         android-shims/.../hyprdevices_android.cpp
services/Hypr.qml           →         android/platform/qml/AndroidHypr.qml
                                      
PipeWire (audio visualizer) →         android-shims/pipewire_android.cpp
                                        (Qt Multimedia / AudioRecord + FFT)
D-Bus (IPC)                 →         Android Binder / JNI (AndroidWindowBridge)
libcava / aubio             →         Android AudioRecord (same shim)
Hyprland window manager     →         Android WindowManager + Magisk freeform
Wayland layer-shell         →         Qt Android fullscreen activity
```

## What runs unchanged

- All `components/*.qml` — UI components, animations, blob shapes
- All `modules/bar/*.qml` — top bar, workspaces, status icons
- All `modules/dashboard/*.qml` — dashboard tabs (Media, Performance, etc.)
- All `modules/launcher/*.qml` — app launcher
- All `modules/notifications/*.qml` — notification center
- All `modules/lock/*.qml` — lock screen
- All `modules/sidebar/*.qml` — side panels
- All assets (fonts, wallpaper, animations)

## What's replaced

| File | Replacement | Reason |
|------|-------------|--------|
| `services/Hypr.qml` | `android/platform/qml/AndroidHypr.qml` | Strips Wayland IPC |
| `plugin/.../hyprdevices.cpp` | `android-shims/.../hyprdevices_android.cpp` | Uses Android InputManager |
| `plugin/.../hyprextras.cpp` | `android-shims/.../hyprextras_android.cpp` | Stubs socket IPC |
| `libpipewire` | `android-shims/pipewire_android.cpp` | Uses Qt Multimedia |
| `Quickshell` framework | `android/platform/qml/Quickshell/` | Android-native QML |

## Build

Push to GitHub → Actions builds the APK automatically.
See `.github/workflows/build-android.yml`.

### Local build (requires Qt 6.8+ for Android)

```bash
cmake -S . -B build \
  -f CMakeLists.android.txt \
  -G Ninja \
  -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-29 \
  -DQT_HOST_PATH=/opt/Qt/6.8.3/gcc_64 \
  -DCMAKE_PREFIX_PATH=/opt/Qt/6.8.3/android_arm64_v8a \
  -DANDROID=ON

cmake --build build --target apk
```

## Requirements

- Android 10+ (API 29+) — freeform window mode APIs
- Magisk or KernelSU — enables freeform + grants system permissions
- Accessibility service enabled — for Hyprland-style keybinds

## Keybinds

| Shortcut | Hyprland equivalent | Action |
|----------|---------------------|--------|
| SUPER + 1–4 | `workspace 1` | Switch workspace |
| SUPER + Q | `killactive` | Close window |
| SUPER + F | `fullscreen` | Toggle fullscreen |
| SUPER + ENTER | `exec app-launcher` | Open app drawer |
| SUPER + Space | — | Media play/pause |
| SUPER + H/J/K/L | `movefocus l/d/u/r` | Focus direction |
| SUPER + SHIFT + 1–4 | `movetoworkspace 1` | Move window to workspace |
| SUPER + TAB | — | Cycle windows |

## Magisk module

`magisk-module/` — flash via Magisk or TWRP.
- Enables Android freeform window mode system-wide
- Grants ReArchLauncher required system permissions at boot
- Persists across reboots

## Source attribution

- `caelestia/shell/` — [Caelestia Shell](https://github.com/caelestia-dots/shell) (GPL-3.0)
- `hyperland/Hyprland/` — [Hyprland](https://github.com/hyprwm/Hyprland) (BSD-3-Clause)
- Android shims + platform bridge — ReArchLauncher contributors
