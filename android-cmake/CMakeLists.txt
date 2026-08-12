cmake_minimum_required(VERSION 3.24)

# ─── Version ──────────────────────────────────────────────────────────────
set(VERSION "1.0.0" CACHE STRING "App version")
set(GIT_REVISION "android-port" CACHE STRING "Git revision")

project(ReArchLauncher VERSION ${VERSION} LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if(NOT ANDROID)
    message(FATAL_ERROR "Use CMakeLists.android.txt for Android builds only.")
endif()

# ─── Qt6 ──────────────────────────────────────────────────────────────────
find_package(Qt6 REQUIRED COMPONENTS
    Core Qml Gui Quick QuickControls2
    Concurrent Sql Network ShaderTools Multimedia
)
qt_standard_project_setup(REQUIRES 6.8)

set(QT_QML_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/qml")

# ─── Paths ────────────────────────────────────────────────────────────────
set(REPO_ROOT       "${CMAKE_SOURCE_DIR}/..")
set(CAELESTIA_SRC   "${REPO_ROOT}/caelestia/shell")
set(ANDROID_PLAT    "${REPO_ROOT}/android/platform")
set(ANDROID_SHIMS   "${REPO_ROOT}/android-shims")
set(PATCHED         "${CMAKE_BINARY_DIR}/caelestia-patched")

# ─── Step 1: Stage Caelestia source (patched copy) ────────────────────────
file(COPY "${CAELESTIA_SRC}/"
     DESTINATION "${PATCHED}"
     PATTERN ".git" EXCLUDE
)
# Swap Hypr.qml with our Android version
file(COPY "${ANDROID_PLAT}/qml/AndroidHypr.qml"
     DESTINATION "${PATCHED}/services/")
file(RENAME "${PATCHED}/services/AndroidHypr.qml"
            "${PATCHED}/services/Hypr.qml")

# ─── Patch reserved-word QML ids ──────────────────────────────────────────
# InputField.qml uses `id: char` — `char` is a reserved word in the QML JS
# engine. Rename to `charEl` throughout. Original source untouched.
file(READ "${PATCHED}/modules/lock/center/InputField.qml" _input_qml)
string(REPLACE "id: char"     "id: charEl"  _input_qml "${_input_qml}")
string(REPLACE "target: char" "target: charEl" _input_qml "${_input_qml}")
string(REPLACE "char."        "charEl."     _input_qml "${_input_qml}")
file(WRITE "${PATCHED}/modules/lock/center/InputField.qml" "${_input_qml}")

# ─── Patch audiocollector.hpp: jthread/stop_token → thread/atomic ─────────
# moc autogen target doesn't always inherit CXX_STANDARD; patch as safety net.
set(_ac_hpp "${PATCHED}/plugin/src/Caelestia/Services/audiocollector.hpp")
file(READ "${_ac_hpp}" _ac)
string(REPLACE "#include <stop_token>" "#include <atomic>" _ac "${_ac}")
string(REPLACE "std::stop_token"  "std::atomic<bool>" _ac "${_ac}")
string(REPLACE "std::jthread"     "std::thread"       _ac "${_ac}")
file(WRITE "${_ac_hpp}" "${_ac}")

# ─── Step 2: Stage all QML into CMAKE_CURRENT_BINARY_DIR so paths are local
#    Qt6 qt_add_qml_module requires relative paths OR QT_RESOURCE_ALIAS.
#    We copy everything to build/staging/ and compute relative aliases.

set(STAGING "${CMAKE_CURRENT_BINARY_DIR}/staging")

# Copy Quickshell shims
file(COPY "${ANDROID_PLAT}/qml/Quickshell/"
     DESTINATION "${STAGING}/Quickshell/")
file(COPY "${ANDROID_PLAT}/qml/AndroidHypr.qml"
     DESTINATION "${STAGING}/")

# Copy Caelestia QML (already patched)
file(COPY "${PATCHED}/"
     DESTINATION "${STAGING}/caelestia/"
     PATTERN "*.qml"
     PATTERN "plugin" EXCLUDE
     PATTERN ".git"   EXCLUDE
)

# ─── Helper macro: register QT_RESOURCE_ALIAS for a list of files ─────────
# For each file under BASE_DIR, sets alias = path relative to BASE_DIR
macro(qt_alias_files OUT_VAR BASE_DIR GLOB_PATTERN)
    file(GLOB_RECURSE _files "${BASE_DIR}/${GLOB_PATTERN}")
    foreach(_f ${_files})
        file(RELATIVE_PATH _alias "${BASE_DIR}" "${_f}")
        set_source_files_properties("${_f}" PROPERTIES QT_RESOURCE_ALIAS "${_alias}")
    endforeach()
    set(${OUT_VAR} ${_files})
endmacro()

# ─── Step 3: Android bridge C++ library ───────────────────────────────────
qt_add_library(rearch_android_bridge STATIC
    "${ANDROID_PLAT}/src/AndroidWindowBridge.cpp"
    "${ANDROID_PLAT}/src/AndroidWindowBridge.hpp"
)
target_include_directories(rearch_android_bridge PUBLIC "${ANDROID_PLAT}/src")
target_link_libraries(rearch_android_bridge PRIVATE Qt6::Core Qt6::Gui Qt6::Qml Qt6::Quick Qt6::Multimedia)

# ─── Step 4: Bridge QML singleton module ──────────────────────────────────
qt_alias_files(BRIDGE_QML_FILES "${STAGING}" "*.qml")  # AndroidHypr + anything at root

qt_add_qml_module(rearch_android_bridge_qml
    URI "com.rearch.android"
    VERSION 1.0
    SOURCES
        "${ANDROID_PLAT}/src/AndroidWindowBridge.cpp"
        "${ANDROID_PLAT}/src/AndroidWindowBridge.hpp"
    RESOURCE_PREFIX "/qt/qml"
)
target_link_libraries(rearch_android_bridge_qml PRIVATE rearch_android_bridge Qt6::Gui)

# ─── Step 5: Quickshell shim QML module ───────────────────────────────────
qt_alias_files(QUICKSHELL_QML "${STAGING}/Quickshell" "*.qml")

qt_add_qml_module(rearch_quickshell_shim
    URI "Quickshell"
    VERSION 1.0
    QML_FILES ${QUICKSHELL_QML}
    RESOURCE_PREFIX "/qt/qml"
)

# ─── Step 6: Caelestia C++ plugin ─────────────────────────────────────────
# Collect ALL plugin subdirectories for include_directories
# (needed by generated qmltyperegistrations.cpp and moc files)
file(GLOB_RECURSE _plugin_headers
    "${PATCHED}/plugin/src/*.hpp"
    "${PATCHED}/plugin/src/*.h"
)
set(_plugin_include_dirs "${PATCHED}/plugin/src")
foreach(_h ${_plugin_headers})
    get_filename_component(_d "${_h}" DIRECTORY)
    list(APPEND _plugin_include_dirs "${_d}")
endforeach()
list(REMOVE_DUPLICATES _plugin_include_dirs)
file(GLOB_RECURSE PLUGIN_SOURCES
    "${PATCHED}/plugin/src/*.cpp"
    "${PATCHED}/plugin/src/*.hpp"
)
list(FILTER PLUGIN_SOURCES EXCLUDE REGEX "hyprdevices\\.cpp|hyprextras\\.cpp")
list(APPEND PLUGIN_SOURCES
    "${ANDROID_SHIMS}/Caelestia/Internal/android/hyprdevices_android.cpp"
    "${ANDROID_SHIMS}/Caelestia/Internal/android/hyprextras_android.cpp"
    "${ANDROID_SHIMS}/pipewire_android.cpp"
)

qt_alias_files(CAELESTIA_QML "${STAGING}/caelestia" "*.qml")

qt_add_qml_module(caelestia_plugin
    URI "Caelestia"
    VERSION 1.0
    SOURCES ${PLUGIN_SOURCES}
    QML_FILES ${CAELESTIA_QML}
    RESOURCE_PREFIX "/qt/qml"
)
target_include_directories(caelestia_plugin PRIVATE
    ${_plugin_include_dirs}
    "${ANDROID_SHIMS}/Caelestia/Internal/android"
    "${ANDROID_SHIMS}"
    "${ANDROID_SHIMS}/include"   # pipewire/, spa/, aubio/ stubs
)
target_link_libraries(caelestia_plugin PRIVATE
    Qt6::Core Qt6::Qml Qt6::Quick Qt6::QuickControls2
    Qt6::Concurrent Qt6::Sql Qt6::Network Qt6::Multimedia
    rearch_android_bridge
)
target_compile_definitions(caelestia_plugin PRIVATE
    ANDROID_BUILD=1 NO_PIPEWIRE=1 NO_DBUS=1 NO_WAYLAND=1 QT_NO_DBUS=1
)
# Force C++20 explicitly — autogen/moc targets don't always inherit CMAKE_CXX_STANDARD
set_target_properties(caelestia_plugin PROPERTIES
    CXX_STANDARD 20
    CXX_STANDARD_REQUIRED ON
    CXX_EXTENSIONS OFF
)
target_compile_options(caelestia_plugin PRIVATE -std=c++20)

# ─── Step 7: Main executable ───────────────────────────────────────────────
qt_add_executable(ReArchLauncher "${ANDROID_PLAT}/src/main.cpp")

# shell.qml alias
set_source_files_properties("${STAGING}/caelestia/shell.qml"
    PROPERTIES QT_RESOURCE_ALIAS "shell.qml")

# Assets — copy to staging first so they're local
file(COPY "${CAELESTIA_SRC}/assets/" DESTINATION "${STAGING}/assets/")

file(GLOB ASSET_FILES
    "${STAGING}/assets/*.webp"
    "${STAGING}/assets/*.gif"
    "${STAGING}/assets/*.svg"
    "${STAGING}/assets/*.ttf"
    "${STAGING}/assets/**/*.ttf"
)
foreach(_a ${ASSET_FILES})
    file(RELATIVE_PATH _alias "${STAGING}" "${_a}")
    set_source_files_properties("${_a}" PROPERTIES QT_RESOURCE_ALIAS "${_alias}")
endforeach()

qt_add_qml_module(ReArchLauncher
    URI "rearch.main"
    VERSION 1.0
    QML_FILES "${STAGING}/caelestia/shell.qml"
    RESOURCES ${ASSET_FILES}
    RESOURCE_PREFIX "/"
)
target_link_libraries(ReArchLauncher PRIVATE
    Qt6::Core Qt6::Qml Qt6::Quick Qt6::QuickControls2
    caelestia_plugin
    rearch_android_bridge
    rearch_android_bridge_qml
    rearch_quickshell_shim
)

# ─── Android deployment ────────────────────────────────────────────────────
set_target_properties(ReArchLauncher PROPERTIES
    QT_ANDROID_PACKAGE_SOURCE_DIR "${ANDROID_PLAT}"
    QT_ANDROID_MIN_SDK_VERSION     29
    QT_ANDROID_TARGET_SDK_VERSION  35
    QT_ANDROID_VERSION_NAME        "${VERSION}"
    QT_ANDROID_ABIS                "arm64-v8a"
)
qt_android_generate_deployment_settings(ReArchLauncher)
qt_android_add_apk_target(ReArchLauncher)
