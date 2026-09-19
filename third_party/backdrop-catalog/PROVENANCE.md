# Upstream Backdrop Catalog integration

Source: Kyant0/AndroidLiquidGlass, Android tag `1.0.0`.
Pinned commit: `eada9a8600142c1bd6a5c5fc143d06f599cdfaef`.
Source URL: https://github.com/Kyant0/AndroidLiquidGlass/tree/eada9a8600142c1bd6a5c5fc143d06f599cdfaef/catalog/src/main/java/com/kyant/backdrop/catalog
License: Apache-2.0, included verbatim in LICENSE. Original source files are kept in
components/ and utils/; source-sha256.json records their original bytes.

Backdrop's upstream README explicitly says the published effect library does NOT
include high-level components. The upstream Catalog supplies LiquidBottomTabs,
LiquidBottomTab, LiquidButton and LiquidToggle as example source components.
The app uses these source components, not a claimed published widget artifact.

Compiled adaptations are under the app's ui/components/upstream package.
The original lens/highlight/shadow layers, CombinedBackdrop composition, spring
constants, velocity distortion and pressedScale (78/56 for tabs, 1.5 for toggle)
are retained. ContinuousCapsule uses upstream's `io.github.kyant0:capsule:2.1.1`.
No custom refraction shader, rainbow border or replacement drag physics is added.

Local adaptations:
- Relocate the package, use the app's explicit light/dark palette and dynamic accent.
- Move Dock interaction observers from the selected thumb onto the entire panel;
  map down coordinates to ANY tab, and consume physical events in Initial pass so
  child click handlers cannot cancel release or prematurely switch pages.
- Commit selection only at release, restore on cancellation, reset when Activity pauses;
  retain semantic Tab click actions and fix RTL translation to use full panel width.
- Extend the upstream observer with cancellation cleanup and explicit consumption
  flags; DampedDragAnimation gets a cancel callback. Springs/lens behavior unchanged.
- Remove the old Android sample's Clear-blend drawBehind (newer upstream also removed
  it) to avoid punching transparent pixels through the backing surface.
- LiquidButton gains enabled/height/padding parameters, clips the interactive highlight
  to ContinuousCapsule, and enables the library's chromaticAberration flag.
- App adapters provide accessible labels and content colors; upload is a tinted
  LiquidButton with a text label, not a transparent Material FloatingActionButton.

Documentation followed:
https://kyant.gitbook.io/backdrop/tutorials/interactive-glass-bottom-bar
In particular: transformations are passed to drawBackdrop.layerBlock so the sampled
background does not scale together with the foreground.
https://kyant.gitbook.io/backdrop/api/backdrop-effects
The chromaticAberration lens option produces the colored refraction; it is not a
painted gradient outline.

Debug verification:
`./gradlew :app:assembleDebug -PglassPreview` builds a separate `.glasspreview` app.
Its GlassPreviewActivity has no account or transfer actions. Never install this
variant over the user's normal application. Normal builds omit that suffix.

Interaction corrections (2026-09-12):
- Dock release now uses a synchronous absolute pointer-to-tab mapping, not the
  coroutine-updated Animatable.targetValue; slight overflow clamps to end tabs
  instead of restoring the original page. Only actual cancellation restores it.
- Toggle pointer handling covers the complete track (48dp minimum hit height),
  remembers the latest selected callback, and separates touch-slop jitter from drag.
- Button drawing reads tint/surface as snapshot state; filter selection changes
  recreate their recording layer so deselected controls cannot retain blue pixels.
- Release incorporates final pointer movement before committing.
