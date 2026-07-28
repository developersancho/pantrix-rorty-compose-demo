# Pantrix Rorty — Compose demo

A from-scratch **Jetpack Compose / Navigation 3 / MVI** sample app that integrates the published
[Pantrix Android SDK](https://github.com/developersancho/pantrix-sdk-android-aar) (`1.0.0-beta.6`) and
exercises every SDK surface, using the
[Rick & Morty REST API](https://rickandmortyapi.com/documentation#rest) as its data source.

It exists because the other three demos leave one question unanswered: **how does a modern Compose app
see this SDK?** `pantrix-rorty-and-demo` is Views + Fragments + OkHttp; the two iOS demos are UIKit and
SwiftUI. The SDK ships first-class modules for Compose, Navigation 3 and Ktor — `pantrix-compose`,
`pantrix-compose-navigation3`, `pantrix-ktor` — and until this app, the only thing using them was the
SDK's own sample. Nothing consuming a **published** release did.

That turned out to matter: every finding at the bottom of this file came from building it.

## Stack

| Concern | Choice |
| --- | --- |
| UI | Jetpack Compose, Material 3, 5 bottom-nav tabs |
| Navigation | **Navigation 3** (`NavDisplay` + `rememberNavBackStack`), one back stack per tab |
| minSdk / compileSdk | 24 / 37 |
| Architecture | **MVI** — `StateFlow` for state, replay-0 `SharedFlow` for effects |
| Layering | clean-ish: `ui` → `domain` ← `data`; only `di` knows both sides |
| DI | **Koin 4.2.2** |
| Networking | **Ktor 3.5.0** (OkHttp engine) + kotlinx.serialization |
| Images | Coil 3 |
| Telemetry | Pantrix — SDK core, Compose, Navigation 3, Ktor, Inspector, Feedback, Widget, Gradle plugin |

**Koin, not Hilt — and not by preference.** Hilt needs KSP, KSP's newest release is `2.3.10`, and this
project is on Kotlin `2.4.10`. There is no KSP for Kotlin 2.4.x. Koin resolves at runtime, so it has no
compiler plugin and no Kotlin-version coupling. Together with kotlinx.serialization (whose plugin ships
inside Kotlin) and Coil 3 (no codegen), the app builds with **zero annotation processors**, which is
exactly what lets it sit on 2.4.10 at all.

---

# Wiring the SDK

## 1. Maven repository — in **two** blocks

`settings.gradle.kts` needs it in `pluginManagement` (for the Gradle plugin) **and** in
`dependencyResolutionManagement` (for the libraries). The `content { includeGroupByRegex(...) }` filter
is not decoration: without it Gradle asks raw.githubusercontent.com about every dependency in the build.

## 2. Dependencies

```kotlin
implementation(libs.pantrix.sdk)                  // core — required
implementation(libs.pantrix.compose)              // trackClick(s) / trackedClick / TrackScroll / TrackInteractions
implementation(libs.pantrix.compose.navigation3)  // PantrixScreenNavTracking
implementation(libs.pantrix.ktor)                 // install(PantrixKtor)

debugImplementation(libs.pantrix.inspector)       // + feedback, widget
releaseImplementation(libs.pantrix.inspector.noop)
```

**Every add-on declares its peers `compileOnly`.** The published POMs list only `kotlin-stdlib`. Compose
UI, nav3 runtime/ui, Ktor core and engine, Glance — all of them come from the app. If a Pantrix symbol
fails to resolve, the missing thing is a peer, not a Pantrix artifact.

Two peers are easy to miss because they fail late rather than at build time:

- **`net.zetetic:sqlcipher-android`** whenever `storageEncryption` is `DATABASE` or `FULL`. Without it
  `Pantrix.init` throws, catches, logs *the SDK is disabled* and the app runs on perfectly — see the
  findings below for how that shipped.
- **`androidx.glance:glance-appwidget`** wherever `pantrix-widget` is. The widget's receiver arrives
  through the merged manifest with no init call, so it appears in the launcher's picker on any build
  type that has the module — and hits `NoClassDefFoundError` when placed, not when built.

## 3. One Pantrix project per build variant

| variant | applicationId | Pantrix project | minified | obfuscated | storage |
| --- | --- | --- | --- | --- | --- |
| `debug` | `…compose.debug` | Rorty Compose Dev | no | no | plaintext |
| `qaTest` | `…compose.test` | Rorty Compose Test | yes | **yes** | SQLCipher |
| `release` | `…compose` | Rorty Compose | yes | yes | SQLCipher |

The ingest gate compares the batch's `build.appId` against the project's recorded `app_id` with exact
equality, so a token from the wrong project does not half-work — the whole batch is rejected.

The **SDK ingest key** lives in `BuildConfig`: it ships inside the app and is extractable from any APK,
so it is public by design. The **CI key**, which uploads the R8 mapping and must stay secret, lives in
the gitignored `local.properties` and never reaches the APK.

## 4. Screens: one line, and no second one

```kotlin
val backStack = rememberNavBackStack(CharactersPage)
PantrixScreenNavTracking(backStack)
NavDisplay(backStack = backStack, /* … */)
```

Navigation 3 has no `NavController` and no graph — the back stack is a list the app owns — so tracking it
is an observing composable rather than a listener registration.

**Do not also call `TrackScreen(...)` inside an entry.** `PantrixScreenNavTracking` already emits a
`screen_view` *and* updates the SDK's current screen, which is what attributes every later click, HTTP
call and crash. A second call double-counts.

The screen name is `key::class.simpleName` — the **NavKey type name**, never its argument values, so an
id in a key stays on the device. That is why the keys are named `CharactersPage`, `CharacterDetailPage`,
and why renaming one renames a metric.

## 5. Interactions: three click paths, and when each is right

| API | Use on | Why |
| --- | --- | --- |
| `Modifier.trackClick` / `trackClicks` | `Row`, `Column`, `Box` — anything with no handler of its own | It installs the only `clickable` there is |
| `trackedClick(name) { }` | `Button`, `FilterChip`, `IconButton`, `Card(onClick=)` | It wraps the handler the component already owns |
| a plain lambda | `NavigationBarItem` | The tab switch is already a `screen_view`; a click event would be noise |

Getting this backwards is not a compile error — putting `Modifier.trackClick` on a `Button` installs a
**second** clickable over the one it already has.

`TrackInteractions(name, interactionSource)` covers what no click modifier can see: **hover, focus and
drag**. Share the source the component already uses; the SDK watches, it never intercepts. In this app it
is on the search fields (a text field gains focus without ever being clicked — keyboard, D-pad, autofill)
and on the Crash Lab's duration slider (the one control here that produces a real `ui_drag`).

`TrackScroll` has two overloads and they report different things:

| state | payload | where |
| --- | --- | --- |
| `LazyListState` | `firstVisibleItem` | the three tab lists |
| `ScrollState` | `scrollOffset` in px | the detail screens, the Profile column |

Both fire once per **settled** gesture, not per frame.

## 6. HTTP through Ktor

```kotlin
HttpClient(OkHttp) {
    install(PantrixKtor)
    install(ContentNegotiation) { json(...) }
}
```

That is the whole integration — `PantrixKtorConfig` has no members; everything is configured on
`PantrixConfig`. Body capture is driven by `trackHttpBody`, which **remote config can override**; the app
asks with `Pantrix.isHttpBodyTrackingEnabled()` rather than reading its own flag.

## 7. R8: nothing to write

The Views demo needs `-keepnames` for its Fragments and Activities. In Compose the screen identity moved
to the NavKeys — and **`pantrix-compose-navigation3` already ships the rule as a consumer rule**, so this
app's `proguard-rules.pro` contains no keep rules at all. Measured, not assumed: see the findings.

---

# Build & run

```bash
./gradlew :app:assembleQaTest
```

```bash
./gradlew :app:installQaTest
```

Needs a Pantrix TEST backend on `http://localhost:8099`, reachable from the emulator at `10.0.2.2:8099`.
A physical device needs the Mac's LAN IP in `PANTRIX_URL` instead.

Note the two URLs are deliberately different: the Gradle plugin uploads the mapping from the **build
machine** (`localhost:8099`), the SDK runs on the **device** (`10.0.2.2:8099`). Swapping them costs a
build-long socket timeout.

# Verifying the integration

1. **On the device** — the Inspector's floating button (dev variants) lists events, HTTP calls, screens
   and crashes straight from the local store.
2. **In the dashboard** — the variant's project should show sessions, screen views and HTTP events. A
   change made in the dashboard's SDK Config screen reaches the device only on the **next launch**, and
   the first request of a cold install can go out before the fetched config is applied — so its body may
   be missing while every later one has it.
3. **Crash path** — Crash Lab triggers a real crash; relaunch (fatal crashes are reported on the next
   launch); the crash should appear in the rollup **deobfuscated**.

   Verified end to end on `qaTest`. The device's own stack was

   ```
   java.lang.IllegalStateException: CrashLab: three frames deep
       at i3.b(r8-map-id-c535be…:130)
   ```

   and the rollup row was

   | build | class | method | line | file |
   | --- | --- | --- | --- | --- |
   | `…compose.test` | `…ui.lab.CrashLabScreenKt` | `crashDeepC` | 139 | `CrashLabScreenKt.java` |
   | `…compose.debug` | `…ui.lab.CrashLabScreenKt` | `crashDeepC` | 96 | `CrashLabScreen.kt` |

   The failure mode to know about: on a minified variant the plugin stamps a mapping id into the APK, so
   if the mapping never arrives the backend marks the crash `symbolication.status = "missing"` and the
   crash rollup **filters it out entirely** — present in the events table, absent from the crash list,
   which reads exactly like a crash that was never captured.

# App structure

```
app/src/main/java/com/pantrix/demo/rorty/compose/
  MainActivity.kt        the only Activity — theme + RortyNavDisplay
  app/                   BuildVariant · RortyApp (init) · NavKeys · RortyNavDisplay · ThemeController
  core/mvi/              MviViewModel · PagedListViewModel · DetailViewModel · IdListViewModel
  domain/                entity (no serialization) · repository (interface) · usecase
  data/                  dto (@Serializable) · mapper · remote/RickMortyClient (Ktor) · repository
  di/                    appModules · dataModule · viewModelModule
  ui/
    characters/          list + status filter    (Contract · ViewModel · Screen)
    locations/           list                     — same chain, no filter
    episodes/            list                     — same chain, no filter
    detail/              three detail screens over one generic scaffold
    crosslist/           a character's episodes · an episode's cast · a location's residents
    profile/             the whole identity surface + theme + "this build"
    lab/                 LabScreen (one row per SDK surface) + CrashLabScreen (7 real crashes)
    shared/              PagedListScaffold · ListRow · ActionRow
```

The layer rule is mechanical: no view model takes a repository or an `HttpClient`, only use cases — so a
`data` type appearing in a constructor would be visible in `di/ViewModelModule.kt`.

# SDK findings this demo raised

Every one of these was measured on a device or in ClickHouse. None of them threw, and most of them looked
fine right up until someone asked the data a question.

### The release build shipped with the SDK entirely disabled

`storageEncryption(FULL)` needs SQLCipher, which the SDK declares `compileOnly`. This app asked for FULL
on release and did not ship the dependency, so `Pantrix.init` threw `IllegalStateException`, caught it,
logged *the SDK is disabled* — and release sets `enableLogging(false)`, so even that line was invisible.
The app ran perfectly and reported nothing. ClickHouse had **zero rows** for the release project while
debug and qaTest looked healthy.

Two fixes came out of it. In the app, SQLCipher now ships on **qaTest as well as release**, because a
storage mode only release uses is a storage mode nobody tests. In the SDK, changing `storageEncryption` on
an install that already has data left the database permanently unopenable — and the SDK retried opening
it roughly every 400ms, forever. Both are fixed in the SDK's `Unreleased` section: the mismatched file is
now discarded so the install keeps reporting, and three consecutive failures stop the retry.

### `qaTest` was not obfuscated, so it could not have caught an obfuscation bug

AGP turns obfuscation **off** for a debuggable build type even when `isMinifyEnabled = true`. While
`qaTest` had `isDebuggable = true` it showed readable screen names no matter what the ProGuard rules said,
so the R8 check the plan called for would have passed for the wrong reason. `qaTest` is now
`isDebuggable = false` — 200 of the app's 213 classes are renamed, and it is a faithful stand-in for release.

### The `-keepnames` rule for NavKeys is the SDK's job, not the app's

Written into `proguard-rules.pro` first, then mutation-tested by deleting it: nothing changed.
`pantrix-compose-navigation3` ships a byte-identical rule as a consumer rule, so it arrives with the
dependency. On a release `mapping.txt` all 11 NavKeys are `X -> X` while `BuildVariant -> cj` and
`RootTab -> g62`; a probe object that implemented `NavKey` survived and a byte-identical one that did not
was renamed to `s43`.

### `NavDisplay` does not give entries their own `ViewModelStore` by default

Opening Rick (id 1), going back, then opening Morty (id 2) produced two `character_opened` events but only
**one** HTTP request — `/api/character/1` — and the second screen rendered Rick. Without
`rememberViewModelStoreNavEntryDecorator()`, `koinViewModel()` resolves against the Activity, so one
`CharacterDetailViewModel` (built with the first id) served every detail entry. Nothing failed; the screen
was simply wrong. Naming `entryDecorators` replaces the defaults, so the saveable-state decorator has to be
restated alongside it.

### Re-pushing an equal NavKey emits no `screen_view`

`PantrixScreenNavTracking` keys its effect on the top NavKey, and NavKeys are data classes. Pushing
`CharacterDetailPage(1)` on top of `CharacterDetailPage(1)` produced a `ui_click` and no `screen_view` —
and its matching pop was silent too. It is self-consistent rather than wrong (the screen name never
changed, so nothing is mis-attributed; a screen visited twice counts once) and only bites a destination
that can re-open itself.

### The Ktor-vs-OkHttp difference is `protocol`, not the lowercased method

The expectation going in was that `pantrix-ktor` lowercases the HTTP method where OkHttp does not.
It does not: **`method` is lowercase for every client**, `okhttp` and `alamofire` included, and even when
`trackHttp` is handed `"GET"` by hand — the SDK normalises it. The real difference is `protocol`: across
every event this backend holds, `okhttp` reports `HTTP_2` when it can and `urlsession` reports `h2`/`h3`,
while `ktor` has reported it on **none** of its requests. Ktor abstracts the engine away; the OkHttp
interceptor sits close enough to the socket to know. `dnsAddress` goes the same way.

### R8's inlined frames are not expanded

The retrace is correct — `i3.b` → `CrashLabScreenKt.crashDeepC` at line 139 — but `crashDeepA` and
`crashDeepB` are missing from the reported stack, even though `mapping.txt` records the inline chain for
that method (`crashDeepC():139 → crashDeepB():138 → crashDeepA():137`). The blame frame and the line are
right, so grouping is unaffected; what is lost is two frames of context the mapping could have restored.
