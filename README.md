# iCamera

<p align="center">
  <img src="assets/banner.svg" alt="iCamera — iPhone-inspired Android camera" width="360" />
</p>

<p align="center">
  <strong>An iPhone-inspired camera experience for Android, built with Kotlin + Jetpack Compose.</strong>
</p>

<p align="center">
  <a href="#overview">Overview</a> ·
  <a href="#goals">Goals</a> ·
  <a href="#hardware-adaptation">Hardware Adaptation</a> ·
  <a href="#architecture">Architecture</a> ·
  <a href="#roadmap">Roadmap</a>
</p>

> **Project status:** Early development / foundation phase.

## Overview

**iCamera** is an Android camera application designed to reproduce the top-level experience of the iPhone Camera app while taking advantage of the hardware and camera capabilities available on each Android device.

The goal is not to copy a fixed UI and assume every phone has the same camera hardware. iCamera should behave like a polished camera product that **adapts itself to the device**:

- Ultra-wide controls appear only when an ultra-wide camera is actually available.
- High-resolution capture modes appear only when supported by the device.
- Video quality and frame-rate options are derived from the camera's real capabilities.
- Camera controls adapt to available lenses, sensors, stabilization modes, zoom ranges, resolutions, frame rates, and other hardware features.
- Unsupported functionality is hidden or disabled rather than presented as a broken control.

The result should feel familiar to an iPhone user while remaining technically native to Android.

## Vision

Build a camera app that combines:

1. **iPhone-like interaction design**
2. **Native Android camera capabilities**
3. **Hardware-aware feature discovery**
4. **High-quality photo and video capture**
5. **A clean, responsive Jetpack Compose UI**
6. **A modular architecture that can evolve with new devices and camera APIs**

The product should prioritize the camera experience itself: fast startup, predictable controls, smooth transitions, reliable capture, and sensible defaults.

## Core Principles

### 1. Hardware is the source of truth

The UI must never assume that a feature exists because another phone supports it.

Capabilities should be discovered at runtime from Android's camera stack and represented as structured camera capabilities.

### 2. Adaptive UI

The interface should dynamically show, hide, or adjust controls based on the selected camera and its capabilities.

For example:

| Capability | Device supports it | Device does not support it |
|---|---|---|
| Ultra-wide lens | Show ultra-wide option | Hide ultra-wide option |
| Telephoto lens | Show telephoto/zoom option | Hide dedicated telephoto option |
| High-resolution stills | Offer supported resolution | Use best supported resolution |
| 4K video | Offer 4K | Hide 4K |
| 60 FPS | Offer 60 FPS where valid | Hide 60 FPS |
| Optical stabilization | Enable/use it | Fall back to supported stabilization |
| RAW capture | Offer RAW mode | Hide RAW mode |

### 3. Capability-driven, not device-model-driven

Avoid hardcoding behavior around device names such as "Pixel 9" or "Galaxy S25".

Prefer capability checks based on Android camera metadata and runtime characteristics.

### 4. Graceful degradation

A device with one camera should still have a complete camera experience.

A flagship device with multiple lenses and advanced capture capabilities should expose more functionality without changing the fundamental UX.

### 5. Performance matters

Camera preview and capture are latency-sensitive. UI architecture, image processing, threading, memory usage, and camera lifecycle management must be designed with real-time performance in mind.

## Planned Camera Experience

The primary camera screen should follow the familiar iPhone Camera information hierarchy:

- Full-screen camera preview
- Minimal top controls
- Flash / torch controls where supported
- Camera switching
- Live-photo-inspired functionality where technically feasible
- HDR / computational imaging indicators where relevant
- Zoom controls derived from available lenses
- Photo / video mode selector
- Shutter button
- Camera switch button
- Recent media preview
- Mode-specific controls
- Contextual settings

The exact visual implementation should be refined against the current iPhone Camera interaction model while keeping Android conventions and platform limitations in mind.

## Hardware Adaptation

iCamera should build a runtime capability model for every available camera.

### Camera discovery

The camera layer should inspect available cameras and determine properties such as:

- Front / back facing
- Logical vs physical camera
- Lens characteristics
- Focal length
- Sensor characteristics
- Active array size
- Available zoom range
- Supported output formats
- Supported still-image resolutions
- Supported video resolutions
- Supported frame rates
- Dynamic range capabilities
- Stabilization support
- Flash availability
- Torch availability
- RAW support
- Manual controls
- Focus capabilities
- Exposure capabilities
- White-balance capabilities
- High-speed video support
- Other Camera2 / CameraX capabilities exposed by the device

### Lens handling

The app should distinguish between:

- Main / wide camera
- Ultra-wide camera
- Telephoto camera
- Front camera
- Other physical cameras where useful

Logical multi-camera devices should be handled carefully so the app does not duplicate or expose confusing controls for physical cameras that Android already groups behind a logical camera.

### Zoom

Zoom controls should be based on the actual available camera characteristics rather than arbitrary fixed values.

The UX can present familiar zoom stops such as:

- 0.5×
- 1×
- 2×
- 3×
- 5×

…but only when those stops make sense for the device.

Continuous zoom should use the camera's supported zoom range.

## Photo Capture

The photo pipeline is expected to support, where hardware/API capabilities allow:

- Standard JPEG capture
- High-resolution JPEG
- Ultra-wide capture
- Telephoto capture
- HDR / HDR-like processing
- RAW / DNG capture
- Zero-shutter-lag strategies where feasible
- Flash
- Torch-assisted capture
- Self timer
- Exposure compensation
- Focus control
- Aspect-ratio selection
- Burst capture
- Image metadata preservation
- EXIF metadata
- Orientation handling

Future computational photography features should be designed as independent processing modules rather than being tightly coupled to the UI.

## Video Capture

Video functionality should adapt to each device's supported combinations of:

- Resolution
- Frame rate
- HDR
- Stabilization
- Lens
- Bit rate
- Codec
- Audio configuration

Potential modes include:

- 1080p
- 4K
- Higher resolutions where supported
- 24 FPS
- 30 FPS
- 60 FPS
- High-frame-rate capture where supported
- Stabilized video
- HDR video where supported

The app must validate **combinations** of settings. A device may support 4K and 60 FPS independently while not supporting 4K60 with a particular lens or stabilization mode.

## Android Camera Stack

The implementation should primarily evaluate modern Android camera APIs, with **CameraX and Camera2** used according to the requirements of each feature.

### CameraX

Use CameraX where it provides reliable lifecycle management and device compatibility for:

- Preview
- Image capture
- Video capture
- Camera lifecycle
- Common camera workflows

### Camera2

Use Camera2 where lower-level control or detailed hardware capability inspection is required, including:

- Camera characteristics
- Physical camera information
- Stream combinations
- Advanced capture configuration
- Manual controls
- Hardware-level capabilities
- Device-specific camera behavior

The architecture should keep these details behind a camera abstraction so the Compose UI does not directly depend on Camera2 internals.

## UI & Design

### Design direction

The UI should be:

- Minimal
- Black/dark camera-first
- Edge-to-edge
- Immersive
- Responsive
- Gesture-friendly
- Animation-aware
- Familiar to iPhone Camera users

The preview should dominate the screen. Controls should disappear or simplify when they are not relevant.

### Jetpack Compose

The interface will be implemented using **Jetpack Compose**.

Compose should own:

- Camera screen layout
- Camera controls
- Mode selector
- Zoom controls
- Settings surfaces
- Capture feedback
- Capability-dependent controls
- Animations and transitions

Camera rendering and capture pipelines should remain separated from Compose UI state.

## Architecture

A likely high-level architecture:

```text
┌───────────────────────────────────────┐
│              Compose UI               │
│ Camera Screen / Controls / Settings   │
└───────────────────┬───────────────────┘
                    │ UI State / Events
┌───────────────────▼───────────────────┐
│         Camera Presentation            │
│ ViewModel / State / User Intent        │
└───────────────────┬───────────────────┘
                    │
┌───────────────────▼───────────────────┐
│          Camera Domain Layer           │
│ Capabilities / Modes / Use Cases       │
└───────────────────┬───────────────────┘
                    │
┌───────────────────▼───────────────────┐
│         Camera Abstraction             │
│ CameraX / Camera2 / Device Adapters    │
└───────────────────┬───────────────────┘
                    │
┌───────────────────▼───────────────────┐
│          Android Camera HAL            │
│        Device Cameras / Sensors        │
└───────────────────────────────────────┘
```

The exact package structure can evolve, but the important boundary is that **UI code should not become the camera engine**.

## Suggested Module Responsibilities

A scalable implementation can separate responsibilities into modules or packages such as:

```text
app/
camera/
  core/
  capabilities/
  preview/
  capture/
  video/
  processing/
  controls/
ui/
  camera/
  components/
  settings/
domain/
  model/
  usecase/
data/
  preferences/
  media/
```

This structure is intentionally flexible. The project should avoid premature modularization until real boundaries emerge from implementation.

## Camera Capability Model

A central capability model should normalize hardware information into app-friendly concepts.

Conceptually:

```kotlin
data class CameraCapabilities(
    val lenses: List<LensCapability>,
    val zoomRange: ClosedFloatingPointRange<Float>,
    val photoCapabilities: PhotoCapabilities,
    val videoCapabilities: VideoCapabilities,
    val stabilizationModes: Set<StabilizationMode>,
    val supportsFlash: Boolean,
    val supportsTorch: Boolean,
    val supportsRaw: Boolean,
)
```

The actual model should be designed around the APIs and requirements discovered during implementation rather than copied blindly from this example.

## Dynamic Feature Rules

Every UI feature should have an explicit capability requirement.

Example:

```text
Feature
  ↓
Capability requirement
  ↓
Device capability resolver
  ↓
Available / unavailable
  ↓
Compose UI state
  ↓
Show / hide / disable
```

This prevents hardware-specific conditions from being scattered throughout the UI.

## Media Storage

The application should use Android's modern media-storage APIs and integrate correctly with the system media library.

The implementation should consider:

- MediaStore
- Photo/video metadata
- EXIF
- Scoped storage
- Android version differences
- Pending media states
- Failed capture cleanup
- Permissions
- User-selected storage behavior where applicable

## Permissions

Permissions should be requested only when necessary and in context.

Potential permissions include:

- Camera
- Microphone
- Media access depending on Android version and workflow

The app should not request unrelated permissions.

## Performance Requirements

Camera applications are unusually sensitive to performance regressions.

Important areas:

- Preview frame latency
- Shutter latency
- Camera startup time
- Memory pressure
- Image processing throughput
- Video encoding performance
- UI recomposition
- Lifecycle transitions
- Background work
- Thermal throttling
- Battery consumption

Performance should be measured on real hardware rather than inferred from emulators alone.

## Device Compatibility

The project should be tested across multiple hardware classes:

### Basic device

- Single rear camera
- Basic front camera
- Standard 1080p video

### Mid-range device

- Multiple rear cameras
- High-resolution main sensor
- 4K video
- Hardware stabilization

### Flagship device

- Main + ultra-wide + telephoto
- High-resolution sensor
- Multiple stabilization modes
- HDR / high-frame-rate video
- Advanced camera combinations

The app should remain usable across all three.

## Testing Strategy

Testing should cover both software behavior and hardware capability differences.

### Unit tests

Test:

- Capability parsing
- Capability normalization
- Mode selection
- Zoom mapping
- Supported-resolution selection
- Video configuration validation
- Feature visibility rules

### Instrumentation tests

Test:

- Camera lifecycle
- Permission flows
- Capture flows
- Rotation
- Background/foreground transitions
- Media saving

### Physical-device testing

Test across devices with substantially different camera hardware.

A camera app that only works correctly on one developer phone is not considered complete.

## Development Setup

### Requirements

- Android Studio
- JDK version supported by the selected Android Gradle Plugin
- Android SDK
- A physical Android device is strongly recommended
- USB debugging enabled for hardware testing

### Build

Clone the repository:

```bash
git clone https://github.com/hustlewithvikram/i-camera.git
cd i-camera
```

Open the project in Android Studio and let Gradle synchronize.

Once the Android project is initialized:

```bash
./gradlew assembleDebug
```

Install a debug build on a connected device using Android Studio or:

```bash
./gradlew installDebug
```

## Project Roadmap

### Phase 1 — Foundation

- [ ] Initialize Android project
- [ ] Kotlin + Jetpack Compose
- [ ] Establish project architecture
- [ ] Camera permission flow
- [ ] Camera preview
- [ ] Basic photo capture
- [ ] Basic video capture
- [ ] MediaStore integration

### Phase 2 — Camera Intelligence

- [ ] Camera discovery
- [ ] Capability model
- [ ] Lens classification
- [ ] Zoom capability mapping
- [ ] Supported photo resolution discovery
- [ ] Supported video configuration discovery
- [ ] Stabilization discovery
- [ ] Flash / torch discovery
- [ ] RAW capability discovery

### Phase 3 — iPhone-like Camera UX

- [ ] Camera-first full-screen UI
- [ ] Mode selector
- [ ] Shutter interaction
- [ ] Zoom controls
- [ ] Top control layout
- [ ] Camera switching
- [ ] Capture animations
- [ ] Haptic feedback
- [ ] Gesture interactions
- [ ] Dynamic controls

### Phase 4 — Advanced Capture

- [ ] High-resolution still capture
- [ ] HDR workflows
- [ ] RAW/DNG
- [ ] Advanced stabilization
- [ ] High-frame-rate video
- [ ] HDR video
- [ ] Advanced exposure/focus controls
- [ ] Burst capture
- [ ] Advanced image processing

### Phase 5 — Device Compatibility

- [ ] Multi-device test matrix
- [ ] Logical/physical camera validation
- [ ] Stream-combination validation
- [ ] Device-specific issue tracking
- [ ] Performance profiling
- [ ] Thermal testing
- [ ] Battery testing

### Phase 6 — Polish

- [ ] Accessibility
- [ ] Localization
- [ ] Crash/error handling
- [ ] Startup optimization
- [ ] Camera transition optimization
- [ ] Media reliability
- [ ] Release build configuration

## What This Project Is Not

iCamera is not intended to be:

- A simple camera wrapper around one preview widget.
- A UI-only clone that ignores Android hardware.
- A device-specific camera application.
- A collection of controls that appear regardless of whether the hardware supports them.

The defining characteristic of this project is the combination of **iPhone-inspired UX + Android-native camera intelligence**.

## Engineering Rules

1. Do not hardcode hardware capabilities when they can be discovered.
2. Do not expose controls for unsupported hardware.
3. Do not put Camera2/CameraX implementation details directly into Compose UI.
4. Prefer capability models over device-model conditionals.
5. Validate supported combinations, not just individual capabilities.
6. Test camera features on physical devices.
7. Keep capture reliability ahead of visual polish.
8. Avoid unnecessary permissions.
9. Measure performance on real hardware.
10. Keep the architecture extensible for future camera APIs and Android versions.

## Contributing

Contributions should preserve the project's core principles:

- Hardware-aware
- Capability-driven
- Android-native
- Performance-conscious
- Modular
- Testable
- Consistent with the camera UX

Before adding a feature, determine whether it is:

1. universally available,
2. conditionally available,
3. device-specific,
4. or dependent on a combination of capabilities.

Conditional functionality should flow through the capability system instead of being implemented as scattered UI checks.

## Disclaimer

iCamera is an independent Android project inspired by the interaction model and visual language of modern smartphone camera applications. It is not an Apple product and is not affiliated with Apple Inc.

Names, trademarks, and platform-specific features belonging to their respective owners remain the property of their owners.

## License

License information will be added when the project's licensing decision is finalized.
