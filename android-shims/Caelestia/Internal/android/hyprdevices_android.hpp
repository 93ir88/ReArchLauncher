#pragma once
// Extends the original hyprdevices.hpp interface with Android-specific methods.
// The original hyprdevices.hpp is included via the plugin source tree unchanged.
// This header is only included by the android shim implementation.

namespace caelestia::internal::hypr {

// Forward declared here — actual class definition in hyprdevices.hpp
// We extend HyprDevices with these two private methods via friend declaration
// in the android build's CMakeLists (see android/CMakeLists.txt).
// Both called only from HyprDevices::refreshFromAndroid() which we add.

void hyprDevicesRefreshFromAndroid(class HyprDevices* self);
void hyprDevicesSynthesizeVirtualKeyboard(class HyprDevices* self);

} // namespace caelestia::internal::hypr
