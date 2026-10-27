# `ads` — a self-contained AdMob package

A flat package of plain `object`s, **the same in every project**: no dependency injection, no layering,
and no reference to any class of the app it sits in (only its own layouts through `R`). Copy the
directory, fix the `package` line, call `AdsController.init` — nothing inside it is edited per app.

**`AdsController` is the one entry point screens use** — inline slots, interstitials, back, rewarded,
full-screen native, the bootstrap. Everything else in the package is its machinery. The app's own
side is one file outside the package (`AppAds.kt` here): the Remote Config parameter name and its
default JSON.

Everything is driven from one Firebase Remote Config JSON parameter, so placements and ad unit ids can
be changed without a release. Nothing is hardcoded except Google's published test ids, which
`AdUnitIds` substitutes in debug builds so you never request real inventory off a dev device.

## The one rule

**Consent → SDK → config → any ad.** In that order, always.

`AdsBootstrap` is the only thing that owns the order, and `AdsController.slot()` is the only way a
slot gets loaded — it waits on `AdsBootstrap.whenReady`. That makes the rule a property of the code
rather than of how long your splash screen happens to stay up. If consent says ads may not be
requested, slots are bound with their "off" value and collapse.

## Adopting it in a new project

1. **Gradle plugin.** Apply `org.jetbrains.kotlin.plugin.serialization`. `AdsConfig` is
   `@Serializable`; the serializer is generated at compile time so there is no reflection for R8 to
   break.

   The two ad slot layouts also use `@dimen/_*sdp` for their gutter and vertical gap, so the project
   needs the **sdp library** (`com.intuit.sdp:sdp-android`) too. Those four values in
   `layout_ad_slot_banner.xml` / `layout_ad_slot_native.xml` are the only `sdp` in the bundle — swap
   them for raw dp if the project has no `sdp`. Those two are the package's whole build-file
   requirement.

2. **Dependencies.**
   ```kotlin
   implementation(platform("com.google.firebase:firebase-bom:<current>"))
   implementation("com.google.firebase:firebase-config")
   implementation("com.google.android.gms:play-services-ads:<current>")   // check minSdk in its notes
   implementation("com.google.android.ump:user-messaging-platform:<current>")
   implementation("com.facebook.shimmer:shimmer:0.5.0")
   ```

   **Mediation.** AdMob stays the base network and these go in every project, so copy them across with
   the rest. Each adapter's version is `<partner SDK version>.<adapter revision>` and pulls its partner
   SDK transitively — except Unity, whose SDK Google stopped bundling: keep it on the version the
   adapter was built against. Check each adapter's version against the `play-services-ads` above (the
   current set is built for 25.4.0/25.5.0); a GMA bump means bumping these too.
   ```kotlin
   implementation("com.google.ads.mediation:unity:4.21.0.0")
   implementation("com.unity3d.ads:unity-ads:4.21.0")
   implementation("com.google.ads.mediation:mintegral:17.1.81.1")
   implementation("com.google.ads.mediation:pangle:8.3.0.4.0")
   implementation("com.google.ads.mediation:facebook:6.22.0.1")   // Meta Audience Network
   implementation("com.google.ads.mediation:applovin:13.6.4.2")   // AppLovin network, via AdMob
   implementation("com.yandex.ads.adapter:admob-mobileads:8.6.0.0")
   ```
   Three things come with them:

   - **Two repositories**, in `settings.gradle.kts` — the Mintegral and Pangle SDKs are published to
     neither Maven Central nor Google's repository:
     ```kotlin
     maven("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") {
         content { includeGroupByRegex("com\\.mbridge\\.msdk.*") }
     }
     maven("https://artifact.bytedance.com/repository/pangle/") {
         content { includeGroup("com.pangle.global") }
     }
     ```
   - **AGP 8.9.1 or newer**, because Meta's SDK pulls `androidx.browser:1.9.0`. Older and
     `checkDebugAarMetadata` fails the build outright.
   - **`-dontwarn` for two Pangle classes** in `proguard-rules.pro`; see the comment there. Nothing else
     needs keeping — every adapter ships consumer rules for the classes the Mobile Ads SDK loads by
     name, and so does Yandex's for its four custom-event classes.

   No code: the adapters are discovered by the Mobile Ads SDK at `initialize()`, so `AdsBootstrap` is
   unchanged and consent still reaches them through UMP. The console is where the work is — each
   network needs its ad source mapped per ad unit, and **Yandex is not an AdMob mediation partner**, so
   it goes in as a *custom event* instead: class `com.admob.mobileads.YandexBanner` /
   `YandexInterstitial` / `YandexRewarded` / `YandexNative`, parameter `{"adUnitId": "R-M-XXXXXX-X"}`.
   Both the class name and the `adUnitId` key are case-sensitive.

3. **Firebase.** Drop `google-services.json` into the app module and apply
   `com.google.gms.google-services`. That plugin fails the build outright while the file is missing, so
   do both together. Create the Remote Config parameter (see *App-specific* below) with the same keys
   as the app's default JSON.

4. **AdMob app id in the manifest.** The SDK reads it from the manifest, not from Remote Config, and
   throws on `initialize()` if it is missing or malformed:
   ```xml
   <meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value="ca-app-pub-…~…" />
   ```

5. **Two calls.** From `Application.onCreate` (registers the resume app open ad and the full-screen
   state tracking — no other lifecycle wiring is needed):
   ```kotlin
   AdsController.init(this, AdsSetup(
       remoteConfigKey = AppAds.REMOTE_CONFIG_KEY,
       defaultJson = AppAds.DEFAULT_JSON,
       isDebug = BuildConfig.DEBUG,
       // optional: noResumeAdOver = setOf("SplashActivity"), allowResumeAd = { … }
   ))
   ```
   And from the launcher activity — consent needs an `Activity`, so this cannot live in `Application`:
   ```kotlin
   AdsController.start(this) { /* consent settled */ }
   ```
   **Do not finish that activity while the consent form is up** — it goes down with it. Start the
   bootstrap, run your splash animation alongside it, and await the consent callback before routing on.
   The app's `SplashActivity` shows the shape, with a long safety timeout.

6. **Resources.** The package needs the files that shipped beside it: the five inline native layouts
   `native_ad_medium_without_media_right_cta`, `native_ad_medium_without_media_bottom_cta`,
   `native_ad_medium_left_media_right_cta`, `native_ad_medium_left_media_bottom_cta`,
   `native_ad_large_top_media_bottom_cta` and their `layout_native_ad_skeleton_<name>.xml`,
   `native_ad_full_screen.xml`, `dialog_{ad_loading,full_screen_native_ad}.xml`,
   `layout_ad_slot_{banner,native}.xml`, `view_ad_slot.xml` (`AdSlotView`), the shimmer placeholders, `bg_ad_*` / `bg_native_ad_*` /
   `ic_ad_close` drawables, `colors_ads.xml` (light and `values-night/`, or aliases onto the app's
   palette), `strings_ads.xml`, `styles_ads.xml`, `attrs_ads.xml` and `ids_ads.xml`. The external references are the Poppins fonts (`poppins_regular`,
   `poppins_semibold`, `poppins_bold`) in the native layouts — supply them or change them.

## App-specific — the only places to edit

Nothing inside the package. The app's side:

| Where | What it holds |
| --- | --- |
| `AppAds.kt` (app, outside the package) | `REMOTE_CONFIG_KEY` (e.g. `VideoDownloaderAdsNew`) and `DEFAULT_JSON` — **every** ads key the app reads, every flag 0, every unit blank |
| `Application.onCreate` | `AdsController.init(…)` with an `AdsSetup` — see step 5 |
| the screens | `AdsController.*` calls with their key names |

`AdsConfig` types only the keys the package itself reads (shared units, splash, resume, rewarded,
native colours). Every screen key — `<slot>_banner` / `_native`, `is_show_*`, `admob_<units>_*_id`,
`counter_*` — is read **by name**, so a new screen or placement is a new key in `DEFAULT_JSON` and a
call, never an edit to the package. In debug builds a key that a screen reads but `DEFAULT_JSON` does
not list is logged (`AdsConfig: Unknown ads key …`), which is what catches a typo.

## Using it from a screen

```kotlin
// Inline slot — banner OR native, whichever the console turns on (native wins if both):
//   home_top_banner / home_top_native, unit ids admob_home_banner_id / admob_home_native_id
AdsController.slot(requireActivity(), binding.topAdContainer, "home_top", units = "home")
AdsController.slot(requireActivity(), binding.midAdContainer, "home_mid", units = "home")
AdsController.slot(requireActivity(), binding.bottomAdContainer, "home_bottom", units = "home")
AdsController.forgetSlots("home")                       // onDestroyView / onDestroy
// The container is a FrameLayout (the part is inflated into it) or an AdSlotView from the XML.
// Options: isCurrent narrows when a late ad may still land (else it is only cached), onResult
// reports whether the slot got an ad:
AdsController.slot(activity, binding.adSlotCenter, "exit_center", isCurrent = { dialog.isShowing })
AdsController.slot(this, binding.adSlotTop, "onboarding1_top", units = "onboarding") { loaded -> }

// Interstitial on a button that opens a screen (counter_screen_buttons)
AdsController.interstitial(requireActivity(), "is_show_home_see_all_btn_inter") { openHistory() }
// … on a bottom-nav tab (counter_bottom_nav), or with the placement's own unit
AdsController.interstitial(this, "is_show_home_btn_inter", AdsController.Counter.BOTTOM_NAV) { navigate() }
AdsController.interstitial(requireActivity(), "is_show_locker_unlock_btn_inter",
    unitKey = "admob_locker_inter_id") { openLocker() }
// … any other group of buttons: just another counter key, e.g. counter = "counter_key_press"

// Launch-flow buttons (language Done, onboarding Get Started / Skip): no counter, 5 s wait
AdsController.launchInterstitial(this, "is_show_admob_language_done_btn_inter",
    unitKey = "admob_language_done_btn_inter_id") { next() }

// Back out of an inner screen (counter_back_btn); system back does what the button does
AdsController.routeSystemBack(this, binding.backButton)
binding.backButton.setOnClickListener {
    AdsController.back(this, "is_show_guide_back_inter") { findNavController().navigateUp() }
}

// Rewarded before an action, full-screen native, misc
AdsController.rewarded(activity, "is_show_home_download_rewarded") { download() }
AdsController.fullScreenNative(this, "is_show_admob_onboarding_full_screen_native",
    unitKey = "admob_onboarding_full_screen_native_id") { nextPage() }    // onDone optional
AdsController.skipNextResume()                          // before leaving the app on purpose
AdsController.isOn("is_show_home_download_rewarded")    // e.g. show an "Ad" label
AdsController.hideWhileKeyboardOpen(binding.root, binding.bottomAdContainer)
```

Every `onDone` / action runs exactly once — after the ad, or straight away when the flag is off, the
counter says no, the unit is blank or the bootstrap is not ready. Unit ids: the placement's own key,
falling back to the shared `admob_app_*_id` when blank; debug builds always request Google's test
units. The splash full-screen ad (app open / interstitial by `splash_ad_preference`) is `SplashAds`.

Two tuning knobs that are not app-specific but are worth knowing: `AdClock.kt:24`
`MAX_CACHED_AD_AGE_MS` (one hour — Google's interstitial guide: "ads expire after an hour") and
`NativeAdCache.kt` `MAX_ENTRIES` (8 — the least recently used native is dropped past that; with
many slots on natives a screen you come back to may request a fresh one instead of a cache hit).

## The keys

The Remote Config parameter and its default JSON are the app's (`AppAds.REMOTE_CONFIG_KEY` /
`AppAds.DEFAULT_JSON`); the console value and the per-screen key list live in the app's `docs/`.
The default JSON should ship every inline slot at 0, so a build that never reaches the console shows
no inline ad. In debug, `AdUnitIds` swaps in Google's test ids, so turning a key on is all it takes
to see a test ad.

### Inline slots — two keys per slot

A screen has up to three slots: **top** (under the header), **mid** / **center** (between two blocks
of content) and **bottom** (above the bottom navigation, or the bottom of a sheet). Each slot reads two keys:

| Key | Values |
| --- | --- |
| `<screen>_<position>_banner` | 0 off · 1 standard 320x50 · 2 collapsible (expands from the top) · 3 collapsible (expands from the bottom) · 4 rectangle 300x250 · 5 inline adaptive |
| `<screen>_<position>_native` | 0 off · 1 `medium_without_media_right_cta` · 2 `medium_without_media_bottom_cta` · 3 `medium_left_media_right_cta` · 4 `medium_left_media_bottom_cta` · 5 `large_top_media_bottom_cta` |

- **One ad per slot.** Both keys on → the native shows and the banner is not requested. Both 0 →
  the slot collapses and no loader runs. Any slot can carry either type; the console decides.
- The choice is made after the ads bootstrap (consent + SDK + fetch), so a first launch whose
  fetch lands late still shows what the console asked for. `AdSlots.slot()` inflates the banner part
  (`layout_ad_slot_banner`, edge to edge) or the native part (`layout_ad_slot_native`, 14sdp gutter)
  into the screen's existing ad `FrameLayout`, and swaps them when the type changes.
- Cache keys are per slot and type: `"<screen>_<position>_banner"` / `"_native"`, e.g.
  `home_mid_native`. A banner is reused only for the same mode.

### Sizes

- Banner slot height is fixed per mode before the request, so the shimmer is the ad's height:
  1 = 50dp, 2/3 = the collapsed anchored adaptive height for the slot width, 4 = 250dp,
  5 = always `INLINE_ADAPTIVE_MAX_HEIGHT_DP` (250dp; a shorter creative is centred in it).
- Native heights (10dp padding each side + content, skeleton = ad): 1 = 80dp, 2 = 130dp,
  3 = 140dp (media 190x120), 4 = 220dp (media 200x140), 5 = width x 9/16 + 130dp.
- Modes 1 and 4 are fixed creatives: they need 320dp / 300dp of slot width.

### Rules for the console

- **Collapsible (2/3)** opens expanded *over* the content once, then sits at its collapsed height.
  Google demand only; mediation returns a normal banner. The SDK keeps the overlay open until the user
  closes it, even after the slot is gone — so **do not use 2/3 on onboarding**: an expanded banner on
  page 2 stays on top of page 3. Use them on screens that stay put (Home / Settings top).
- **Never put a button under an ad.** Large combos (native 4/5 or banner 4/5 in both slots) on Splash
  and Onboarding can push Continue / Get Started under an ad on short phones (AdMob policy). One big
  ad per screen; the other slot 0 or small. The code does not block it.
- Layout 1 puts AdChoices top-left (its top-right is the CTA); every other layout top-right.

### Resume, flow and colors

| Group | Keys |
| --- | --- |
| App open ad on return from background | `admob_resume_app_open_id` · `is_show_admob_resume_app_open` |
| Which launch screens run | `is_show_language_screen` · `is_show_onboarding_screen` · `is_show_language_screen_first_session` · `is_show_onboarding_screen_first_session` |
| Native ad colors | `native_ad_tag_text_color` · `native_ad_tag_bg_color` · `native_heading_text_color` · `native_description_text_color` · `native_button_text_color` · `native_button_bg_color` · `native_ad_bg_color` · `native_ad_border_color` · `native_ad_tag_border_color` |

The `*_first_session` pair applies only until `MainActivity` first opens; the other two apply on
every launch after that. Colors are `#RRGGBB` or `#AARRGGBB`; blank or unparseable falls back to the
`native_ad_*` defaults in `colors_ads.xml`.

### Counters

`counter_bottom_nav`, `counter_screen_buttons` and `counter_back_btn` (`AdsController.Counter`), and
any other `counter_*` key a project passes, all read the same way — a missing key reads as 1: **0 never shows an ad, 1 shows on every qualifying click, N shows on every Nth.** Only
a click whose own `is_show_*_inter` flag is on advances its counter, and counters reset on the next
cold start — they are never persisted.

## The console rule: numbers unquoted

```json
"home_top_banner": 1      ✅
"home_top_banner": "1"    ❌ that key reads as 0
```

A quoted number on a screen key reads as its default (off). On one of the package's typed keys
(`splash_ad_preference`, `is_show_admob_splash_*`, `is_show_admob_resume_app_open`) it is a type
error: the parse fails and **every ad in the app goes off**, with one log line under
`RemoteConfigManager`. Unknown keys are ignored, so adding keys the build does not know about is safe.

## Where a slot's spacing lives

Two gaps, two owners, and the split is deliberate.

**Vertical — the package's.** `layout_ad_slot_banner` and `layout_ad_slot_native` each carry
`paddingTop="@dimen/_4sdp"` / `paddingBottom="@dimen/_4sdp"` on their root. An ad has to read as clearly separate from
the app's own content, and on a scrolling screen it does not — the content scrolls right up to the
slot and the two read as one column. Putting it here rather than on each include means a host cannot
miss a slot, and it costs the ads-off case nothing: the loaders collapse *that root*, so the padding
only exists while an ad is showing and a screen whose flag is 0 measures exactly as it did before.
Both edges, because the same include is a top slot on one screen, a bottom slot on another and a mid
slot on a third — "the content-facing edge" is not something the file can know.

**Horizontal — also the slot's.** The gutter lives on the same root:

```xml
android:paddingStart="@dimen/_14sdp"
android:paddingEnd="@dimen/_14sdp"
```

**No screen sets a slot's spacing.** A host's `<include>` carries constraints and nothing else — no
`layout_margin*` of any kind. Both the gutter and the vertical gap are changed in one place, in
`layout_ad_slot_banner.xml` and `layout_ad_slot_native.xml`, and every slot on every screen follows.

That is a deliberate trade: writing `@dimen/_*sdp` here means **the package now requires the `sdp`
library (`com.intuit.sdp`) as well as the Kotlin serialization plugin.** One place to change the
spacing was judged worth one more dependency. If a project adopting this package has no `sdp`, swap
those four values for raw dp — they are the only four in the bundle.

**Every plain-FrameLayout slot is 14sdp**, whatever the screen's own gutter: one rule for every slot
reads better than each screen with its own number. A screen that needs its own native margin uses an
`AdSlotView` instead (below).

**`AdSlotView` — the other kind of slot.** A slot can also be an `AdSlotView` placed straight in the
screen's XML (`view_ad_slot.xml` inside): it holds both parts already, the banner edge to edge and the
native with `app:nativeMarginHorizontal`, and the whole view is the slot that collapses. Its vertical
margins are the host's, on the view itself. `AdsController.slot()` takes either kind.

> **A gutter has a floor if the screen uses banner mode 1.** The standard banner is a fixed 320x50
> creative, centred without scaling, so a slot narrower than 320dp clips it on both sides. A gutter
> of `g` leaves `screenWidthDp - 2g`: fine at 411dp wide, tight at 360dp, and too narrow below that.
> Mode 2 (inline adaptive) has no floor — it asks for whatever width the slot actually has — so it is
> what to move a screen to if its gutter ever has to grow.

## The host's contract

**Must:**

- Call `AdsController.init()` from `Application.onCreate` and `AdsController.start()` from the
  launcher activity, and not finish that activity while consent is showing.
- Load every inline slot through `AdsController.slot()`, one call per slot with its
  `<screen>_<position>` key. Give its container constraints and nothing else — **no `layout_margin*`
  of any kind.** Both the gutter and the vertical gap are the slot's own; see *Where a slot's spacing
  lives*. The one exception is a mid slot inside a host wrapper, where the margin below the slot
  is the wrapper's gap to the content that follows, not the slot's spacing.
- Call `AdsController.slot()` again when a screen becomes visible, not only when it is created. With
  `show()`/`hide()` fragments the view survives, so a bind that runs once at creation leaves an evicted
  or expired ad on screen indefinitely. Re-binding is also what makes the caches worth having.
- Call `AdsController.forgetSlots("<screen>")` from `onDestroy` / `onDestroyView` (it clears that
  screen's top, mid and bottom keys of both types).
- List every key a screen reads in the app's `DEFAULT_JSON`.

**Must never:**

- Call the loaders or managers (`BannerAdLoader`, `NativeAdLoader`, `InterstitialAdManager`, …)
  directly. They do not wait for consent and do not count clicks — `AdsController` does both.
- Treat `AdsController.config()` as fetched before the bootstrap is ready. Read it inside
  `whenReady`, as the slots do.
- Hold a cached `AdView` or `NativeAd` yourself, or call `destroy()` on one. The caches own their
  lifetime: a banner belongs to one Activity and dies with it, a native ad is destroyed when it is
  replaced, expires, is displaced by the LRU cap, or the app is trimmed in the background.
- Give a slot a `<screen>_<position>` key that another slot already uses. Keys are the cache's
  identity: two slots on one key would be handed the same ad, and the cache destroys it under
  one of them the moment the other reloads.

## If the tests come too

`AdCacheLeakTest`, `AdCacheExpiryTest`, `AdsBootstrapWaiterTest`, `AdsConfigDefaultsTest`,
`AdUnitIdsTest`, `FullScreenAdGapTest` and `InlineSlotPredicateTest` — 89 tests covering the cache
lifetimes, expiry, the LRU cap, the activity-scoped waiters, the agreement between `AdsConfig` and
`DEFAULT_JSON`, the test-id substitution, the minimum gap between full-screen ads, and the
slot predicate (now `AdSlots.mayShowAnAd`). They predate `AdsController` and need updating before use:
`AdsConfigDefaultsTest` compared typed fields with `DEFAULT_JSON`, which is now the app's.

`FullScreenAdGapTest` needs only JUnit: it drives `AdClock` by hand and calls the gates directly, with
no `Activity` and no ad object. So do `AdUnitIdsTest` and `InlineSlotPredicateTest` — the latter runs
with no Firebase in the process on purpose, which is the same path a build with no
`google-services.json` takes, and is exactly the "every flag at 0" state it is asserting about.

`AdsConfigDefaultsTest` reads the two source files off disk rather than the compiled classes, so it
needs nothing but JUnit — but it does need the working directory to be the app module, which is
where Gradle already runs it. It is the one that fails when a key is added to one side only, when a
console number is quoted, or when a real ad unit id is committed. The other three need, on the
**test** classpath only:

```kotlin
testImplementation("io.mockk:mockk:<current>")   // AdView / NativeAd / Activity cannot be constructed off a device
```

plus `androidx.activity` and `androidx.lifecycle` already on the compile classpath —
`AdsBootstrapWaiterTest` mocks `ComponentActivity` and drives a real `LifecycleRegistry`.

They also need `testOptions { unitTests.isReturnDefaultValues = true }`, since the caches touch
`android.util.Log`.
