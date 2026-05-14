# EV-Grama Charge

An Android application that connects EV drivers with local village-level charging hosts.  
Drivers can discover nearby charging stations on a live map, book a slot, and estimate charging energy.  
Hosts can register their charging points and manage them from the same app.

---

## Table of Contents

1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Architecture Overview](#architecture-overview)
4. [Project Structure](#project-structure)
5. [File-by-File Explanation](#file-by-file-explanation)
   - [Application Entry Point](#application-entry-point)
   - [Data Layer](#data-layer)
   - [UI Layer – Map](#ui-layer--map)
   - [UI Layer – Bookings](#ui-layer--bookings)
   - [UI Layer – Battery Calculator](#ui-layer--battery-calculator)
   - [UI Layer – Hosts](#ui-layer--hosts)
   - [Main Activity & Navigation](#main-activity--navigation)
   - [Resources](#resources)
   - [Build & Config Files](#build--config-files)
6. [Data Flow Diagram](#data-flow-diagram)
7. [Firestore Database Design](#firestore-database-design)
8. [Security Rules](#security-rules)
9. [Local Setup](#local-setup)
10. [Adding Your First Charging Host (Seed Data)](#adding-your-first-charging-host-seed-data)
11. [Roadmap](#roadmap)

---

## Previewing the App (Evaluation Note)

For security and best practices, the **Google Maps API Key** is not included in the repository. It is managed via `local.properties`. 

To ensure a smooth evaluation experience even without an API key:
- **Map Placeholder:** If a valid API key is not provided, the Map screen will display a professional placeholder UI with instructions, instead of a blank screen.
- **Sample Data:** The app automatically generates **Dummy Charging Hosts** and **Sample Bookings** when the Firestore database is empty. This allows you to explore the UI, navigation, and booking dialog flow immediately upon launch.
- **Fully Implemented Logic:** All production code for Google Maps integration (permissions, markers, camera bounds, location tracking) is fully implemented in `MapFragment.kt` and ready for use with a valid key.

---

## Features

| Feature | Description |
|---|---|
| **Live Map** | Google Maps screen with real-time charging host pins pulled from Firestore |
| **Tap to Book** | Tap any pin on the map to open a booking dialog and request a charging slot |
| **My Bookings** | List of all bookings made by the signed-in user, updated in real time |
| **Battery Calculator** | Offline tool to calculate energy to add and extra driving range |
| **Host Registration** | Any user can register a new charging location with GPS co-ordinates |
| **My Hosts** | List of charging locations owned by the signed-in user |
| **Anonymous Auth** | Firebase anonymous sign-in on first launch — no email/password needed |
| **Material 3 UI** | Green EV-themed Material 3 design with Bottom Navigation |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Architecture | MVVM (Model-View-ViewModel) |
| UI | XML layouts + ViewBinding + Material 3 |
| Navigation | Jetpack Navigation Component |
| Async | Kotlin Coroutines + Flow |
| Backend | Firebase Firestore (real-time NoSQL) |
| Auth | Firebase Authentication (Anonymous) |
| Maps | Google Maps SDK for Android + Fused Location Provider |
| Build | Gradle (Kotlin DSL, AGP 8.7.2, Gradle 8.9) |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 (Android 15) |

---

## Architecture Overview

The app follows the **MVVM** pattern recommended by Google, layered as:

```
┌───────────────────────────────────────────────────┐
│                     UI Layer                       │
│  Fragment  ──observe──►  ViewModel                 │
│  (XML + ViewBinding)      (StateFlow / SharedFlow) │
└───────────────────────┬───────────────────────────┘
                        │ calls
┌───────────────────────▼───────────────────────────┐
│                   Data Layer                       │
│  FirestoreRepository  ──►  Firebase Firestore      │
│  (suspend funs + Flow)     (Cloud NoSQL DB)        │
└───────────────────────────────────────────────────┘
```

Key design decisions:

- **One Repository** (`FirestoreRepository`) is the single source of truth for all Firestore reads and writes.
- The repository exposes **Kotlin `Flow`** for real-time streams (using `callbackFlow` wrapping Firestore `addSnapshotListener`).
- **ViewModels** hold `StateFlow` derived from those flows, scoped to `viewModelScope` with `SharingStarted.WhileSubscribed(5_000)` to cancel the upstream listener when no UI is active and restart it within 5 seconds if the screen comes back (handles screen rotation efficiently).
- **Fragments** collect from `StateFlow` inside `repeatOnLifecycle(STARTED)` so they stop collecting in background and resume on foreground automatically.
- The `Application` class creates the repository **once** (via `by lazy`) and all ViewModels share it via a custom `ViewModelProvider.Factory`.

---

## Project Structure

```
EVGramaCharge/
├── app/
│   ├── build.gradle.kts                    ← App-level Gradle config
│   ├── google-services.json                ← Firebase config (gitignored, add locally)
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml             ← Permissions, API key, activity declaration
│       ├── java/com/evgramacharge/app/
│       │   ├── EVGramaChargeApplication.kt ← Application class, DI root
│       │   ├── data/
│       │   │   ├── model/
│       │   │   │   ├── ChargingHost.kt     ← Data model for a charging station
│       │   │   │   └── Booking.kt          ← Data model for a booking
│       │   │   └── repository/
│       │   │       └── FirestoreRepository.kt ← All Firestore read/write logic
│       │   └── ui/
│       │       ├── main/
│       │       │   └── MainActivity.kt     ← Single Activity host, nav setup
│       │       ├── map/
│       │       │   ├── MapFragment.kt      ← Map UI + booking dialog
│       │       │   └── MapViewModel.kt     ← Hosts StateFlow for map
│       │       ├── bookings/
│       │       │   ├── BookingsFragment.kt ← Bookings list UI
│       │       │   ├── BookingsViewModel.kt← User bookings StateFlow
│       │       │   └── BookingsAdapter.kt  ← RecyclerView adapter
│       │       ├── calculator/
│       │       │   ├── BatteryCalculatorFragment.kt ← Calculator UI
│       │       │   └── BatteryCalculatorViewModel.kt← Pure calculation logic
│       │       └── hosts/
│       │           ├── HostsFragment.kt            ← My hosts list UI
│       │           ├── HostsViewModel.kt            ← Owner hosts StateFlow
│       │           ├── MyHostsAdapter.kt            ← RecyclerView adapter
│       │           ├── HostRegistrationFragment.kt  ← Registration form UI
│       │           └── HostRegistrationViewModel.kt ← Save host + event flow
│       └── res/
│           ├── layout/                     ← All XML layouts (see Resources section)
│           ├── navigation/nav_graph.xml    ← Jetpack nav destinations & actions
│           ├── menu/menu_bottom_nav.xml    ← Bottom navigation items
│           ├── values/
│           │   ├── strings.xml
│           │   ├── colors.xml
│           │   └── themes.xml
│           └── drawable/ic_ev_app.xml      ← Vector app icon
├── build.gradle.kts                        ← Root Gradle (plugin versions)
├── settings.gradle.kts                     ← Module includes, repo config
├── gradle.properties                       ← AndroidX flags, JVM args
├── gradle/wrapper/                         ← Gradle wrapper (8.9)
├── gradlew / gradlew.bat
├── firestore.rules                         ← Firestore security rules
├── local.properties.example                ← Template for local.properties
└── .gitignore                              ← Android-specific ignore rules
```

---

## File-by-File Explanation

### Application Entry Point

#### `EVGramaChargeApplication.kt`

```
EVGramaChargeApplication : Application
└── repository: FirestoreRepository  (lazy singleton)
```

- Extends Android's `Application` class — created once for the lifetime of the process.
- Creates `FirestoreRepository` lazily on first access using Kotlin's `by lazy` delegate.
- All four ViewModels call `(app as EVGramaChargeApplication).repository` in their factory methods, ensuring every ViewModel shares the **same** repository instance (manual DI without Hilt/Dagger).

---

### Data Layer

#### `data/model/ChargingHost.kt`

Represents a physical EV charging location registered by a host user.

| Field | Type | Purpose |
|---|---|---|
| `id` | String | Firestore document ID |
| `name` | String | Display name of the station |
| `address` | String | Human-readable address |
| `latitude` | Double | GPS latitude for map pin |
| `longitude` | Double | GPS longitude for map pin |
| `pricePerKwh` | Double | Charging rate shown on the pin info window |
| `connectorType` | String | e.g. "Type 2", "CCS", "CHAdeMO" |
| `ownerId` | String | Firebase Auth UID of the registering user |
| `createdAt` | Long | Epoch milliseconds — used for client-side sorting |

Key methods:
- `toMap()` — serialises the model to a `Map<String, Any?>` for Firestore writes.
- `fromDoc(id, data)` — factory deserialiser that safely casts Firestore document fields, using safe-cast (`as?`) with fallback defaults to prevent crashes on missing or wrong-type fields.
- `COLLECTION` constant (`"charging_hosts"`) centralises the Firestore collection name.

#### `data/model/Booking.kt`

Represents a booking request made by a driver for a specific charging host.

| Field | Type | Purpose |
|---|---|---|
| `id` | String | Firestore document ID |
| `hostId` | String | Reference to `ChargingHost.id` |
| `hostName` | String | Denormalised name for display without a second Firestore read |
| `userId` | String | Firebase Auth UID of the booking user |
| `startEpochMs` | Long | Requested start time in epoch ms |
| `endEpochMs` | Long | Requested end time in epoch ms |
| `estimatedEnergyKwh` | Double | kWh the driver expects to consume |
| `status` | String | `"PENDING"` on creation; can be updated to `"CONFIRMED"` / `"CANCELLED"` |
| `createdAt` | Long | Epoch ms — used for client-side sorting |

Same `toMap()` / `fromDoc()` pattern as `ChargingHost`.

#### `data/repository/FirestoreRepository.kt`

The **single data access point** for the entire app. All reads and writes go through here.

```
FirestoreRepository
├── observeHosts()                    → Flow<List<ChargingHost>>
├── observeBookingsForUser(userId)    → Flow<List<Booking>>
├── observeHostsForOwner(ownerId)     → Flow<List<ChargingHost>>
├── saveHost(host)                    → suspend: Result<String>
└── createBooking(booking)            → suspend: Result<String>
```

**How the live flows work:**

```
callbackFlow {
    val reg = db.collection(...).addSnapshotListener { snapshot, error →
        trySend(mappedList)   // pushes new value into the flow
    }
    awaitClose { reg.remove() }  // cleans up Firestore listener when flow is cancelled
}
```

`callbackFlow` bridges Firestore's callback-based `SnapshotListener` into a Kotlin `Flow`. When the `Flow` is collected, the Firestore listener is active; when the flow is cancelled (e.g. app in background), `awaitClose` removes the listener — no memory leaks.

**Why no composite Firestore indexes?**  
Queries that use both `whereEqualTo` and `orderBy` on different fields require a Firestore composite index. To avoid forcing users to deploy indexes, sorting is done **client-side** (`.sortedByDescending { it.createdAt }`).

**Write methods** use `kotlinx-coroutines-play-services`'s `.await()` extension to turn the Firestore `Task<T>` into a coroutine-friendly suspend call. `runCatching` wraps them so the caller always gets a `Result<String>` rather than an exception.

---

### UI Layer – Map

#### `ui/map/MapViewModel.kt`

```
MapViewModel : AndroidViewModel
└── hosts: StateFlow<List<ChargingHost>>
      ← repository.observeHosts()
         wrapped with stateIn(WhileSubscribed(5_000))
```

- Exposes a single `StateFlow` of all charging hosts.
- `SharingStarted.WhileSubscribed(5_000)`: the upstream Firestore listener stays alive for 5 seconds after the last collector disappears (handles screen rotation without dropping and restarting the network subscription).
- Uses a manual `ViewModelProvider.Factory` to inject the repository from the `Application`.

#### `ui/map/MapFragment.kt`

The most complex fragment. Responsibilities:

1. **Map initialisation** — gets a `SupportMapFragment` child, calls `getMapAsync`, enables zoom controls.
2. **Runtime permission** — requests `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` using `ActivityResultContracts.RequestMultiplePermissions()`. On grant, calls `enableMyLocationLayer()` which activates the blue dot on the map.
3. **My Location FAB** — taps the FusedLocationProviderClient to get last known location and animates the camera there.
4. **Marker management** (`renderMarkers`) — diffs the incoming host list against `markerByHostId` map:
   - Removes stale markers for hosts that no longer exist.
   - Updates position/title/snippet for existing markers.
   - Adds new markers (azure-coloured pins) and stores `ChargingHost` as the marker's `tag`.
   - On first load, fits the camera to all pin bounds (or zooms to zoom-14 if only one pin).
5. **Booking dialog** (`showBookingDialog`) — tapping a pin opens a `MaterialAlertDialog` with two `EditText` fields: duration in hours and estimated kWh. On confirm, calls `submitBooking`.
6. **Booking submission** (`submitBooking`) — builds a `Booking` object from the inputs, calls `repository.createBooking()`, shows a toast on success.
7. **Lifecycle safety** — `_binding` is set to `null` in `onDestroyView` and map/markers are cleared to prevent leaks.

---

### UI Layer – Bookings

#### `ui/bookings/BookingsViewModel.kt`

```
BookingsViewModel : AndroidViewModel
├── userIdFlow: Flow<String?>          ← Firebase AuthStateListener wrapped in callbackFlow
└── bookings: StateFlow<List<Booking>>
      ← userIdFlow.flatMapLatest { uid →
            if uid == null → flowOf(emptyList())
            else → repository.observeBookingsForUser(uid)
         }
```

- Observes Firebase auth state changes via `FirebaseAuth.AuthStateListener` in a `callbackFlow`.
- Uses `flatMapLatest` to automatically switch the Firestore subscription whenever the user's UID changes (handles the case where anonymous sign-in completes after the ViewModel is created).

#### `ui/bookings/BookingsFragment.kt`

- Sets up `RecyclerView` with `BookingsAdapter`.
- Collects `bookings` StateFlow inside `repeatOnLifecycle(STARTED)`.
- Toggles visibility between the RecyclerView, empty-state `TextView`, and `CircularProgressIndicator`.

#### `ui/bookings/BookingsAdapter.kt`

- `ListAdapter<Booking, VH>` with `DiffUtil` for efficient RecyclerView updates.
- Binds `ItemBookingBinding` (generated from `item_booking.xml`).
- Formats `startEpochMs` / `endEpochMs` using `java.text.DateFormat` for locale-aware display.

---

### UI Layer – Battery Calculator

#### `ui/calculator/BatteryCalculatorViewModel.kt`

Purely offline calculation — no network calls, no repository dependency.

```kotlin
fun calculate(
    batteryKwh: Double,
    currentSocPercent: Double,
    targetSocPercent: Double,
    consumptionKwhPer100km: Double,
): BatteryCalcResult?
```

**Formula:**
```
deltaPercent  = targetSoc - currentSoc
energyToAdd   = batteryKwh × (deltaPercent / 100)
extraRangeKm  = energyToAdd / consumptionKwhPer100km × 100
```

Returns `null` (invalid) if battery capacity or consumption is zero, or if target ≤ current SoC.

#### `ui/calculator/BatteryCalculatorFragment.kt`

- Pre-fills sensible defaults (60 kWh battery, 20% → 80% SoC, 16 kWh/100 km consumption).
- On button click: parses inputs, calls `viewModel.calculate()`, shows result or a `Snackbar` error.
- Displays: energy to add in kWh + approximate extra range in km.

---

### UI Layer – Hosts

#### `ui/hosts/HostsViewModel.kt`

Same auth-reactive pattern as `BookingsViewModel`, but filters Firestore by `ownerId` using `observeHostsForOwner(uid)`.

#### `ui/hosts/HostsFragment.kt`

- Shows the list of hosts registered by the current user via `MyHostsAdapter`.
- `ExtendedFloatingActionButton` navigates to `HostRegistrationFragment` using the Navigation Controller.

#### `ui/hosts/MyHostsAdapter.kt`

`ListAdapter<ChargingHost, VH>` — binds `ItemMyHostBinding`, shows name, address, and a combined "connector · price/kWh" meta line.

#### `ui/hosts/HostRegistrationViewModel.kt`

```
HostRegistrationViewModel : AndroidViewModel
├── events: SharedFlow<UiEvent>   ← one-shot events (Saved | Error)
└── saveHost(name, address, lat, lng, price, connector)
      → validates fields
      → builds ChargingHost with ownerId = currentUser.uid
      → repository.saveHost(host)
      → emits UiEvent.Saved or UiEvent.Error
```

- Uses `SharedFlow` (not `StateFlow`) for one-shot UI events because `Saved` must only trigger navigation once — `StateFlow` would re-deliver the last value to new collectors.
- `extraBufferCapacity = 1` ensures an event emitted before the Fragment starts collecting is not lost.

#### `ui/hosts/HostRegistrationFragment.kt`

- Renders a scrollable form with `TextInputLayout` / `TextInputEditText` fields.
- On save: disables button and shows `LinearProgressIndicator`.
- Collects `viewModel.events`:
  - `Saved` → shows toast, calls `findNavController().popBackStack()` to return to Hosts screen.
  - `Error` → shows `Snackbar`, re-enables the save button.

---

### Main Activity & Navigation

#### `ui/main/MainActivity.kt`

The **single Activity** that hosts all Fragments.

Responsibilities:
1. Sets `ActivityMainBinding` as content view.
2. Connects the `NavController` (from `NavHostFragment`) to:
   - `MaterialToolbar` via `setupWithNavController` — auto-updates title and back button.
   - `BottomNavigationView` via `setupWithNavController` — manages fragment back-stack for each tab.
3. Sets `AppBarConfiguration` with the four top-level destination IDs so the back arrow does not appear on the bottom-nav screens.
4. Calls `ensureAnonymousAuth()` in a coroutine — signs in anonymously if no user session exists.

#### `res/navigation/nav_graph.xml`

Defines the Navigation graph:

```
startDestination: mapFragment
├── mapFragment           (MapFragment)
├── bookingsFragment      (BookingsFragment)
├── batteryCalculatorFragment (BatteryCalculatorFragment)
├── hostsFragment         (HostsFragment)
└── hostRegistrationFragment  (HostRegistrationFragment)
     ↑ navigated to from hostsFragment via FAB
```

`hostRegistrationFragment` is not a bottom-nav item — it is a detail screen pushed on top of the Hosts tab.

#### `res/menu/menu_bottom_nav.xml`

Four `<item>` entries whose `android:id` values **match exactly** the fragment IDs in `nav_graph.xml`. This match is how `BottomNavigationView.setupWithNavController` knows which fragment to show for each tab.

---

### Resources

| File | Purpose |
|---|---|
| `layout/activity_main.xml` | Root layout: `MaterialToolbar` + `FragmentContainerView` + `BottomNavigationView` in a vertical `LinearLayout` |
| `layout/fragment_map.xml` | `FragmentContainerView` for `SupportMapFragment` + `CircularProgressIndicator` overlay + location FAB |
| `layout/fragment_bookings.xml` | `RecyclerView` + empty `TextView` + `CircularProgressIndicator` (all `ConstraintLayout`) |
| `layout/item_booking.xml` | `MaterialCardView` with 4 `TextViews`: host name, status badge, time window, energy |
| `layout/fragment_battery_calculator.xml` | `ScrollView` → `LinearLayout` with 4 `TextInputLayout` fields + calculate button + result `MaterialCardView` |
| `layout/fragment_hosts.xml` | `CoordinatorLayout` with `RecyclerView` + empty `TextView` + `ExtendedFloatingActionButton` |
| `layout/item_my_host.xml` | `MaterialCardView` with host name, address, connector/price meta |
| `layout/fragment_host_registration.xml` | `ScrollView` form: 6 `TextInputLayout` fields + save button + `LinearProgressIndicator` |
| `values/themes.xml` | `Theme.Material3.DayNight.NoActionBar` base, custom green `colorPrimary` |
| `values/colors.xml` | Material 3 green palette (`#006C4C` primary) |
| `values/strings.xml` | All user-visible strings, including `booking_energy_fmt` format string |
| `drawable/ic_ev_app.xml` | Vector app icon — green background with a white lightning bolt |

---

## Data Flow Diagram

### Real-time Hosts on Map

```
Firestore "charging_hosts" collection
        │  addSnapshotListener (live updates)
        ▼
FirestoreRepository.observeHosts()   [callbackFlow]
        │  Flow<List<ChargingHost>>
        ▼
MapViewModel.hosts                   [StateFlow, WhileSubscribed]
        │  collect inside repeatOnLifecycle(STARTED)
        ▼
MapFragment.renderMarkers()
        │  diffs marker map, adds/updates/removes pins
        ▼
GoogleMap (pins visible on screen)
```

### Booking a Slot

```
User taps a map pin
        │
MapFragment.showBookingDialog(host)  [MaterialAlertDialog]
        │  user fills hours + kWh, taps "Book"
        │
MapFragment.submitBooking()
        │  builds Booking(status="PENDING", userId=currentUser.uid)
        ▼
FirestoreRepository.createBooking()  [suspend + runCatching]
        │  Firestore write → new doc in "bookings" collection
        ▼
BookingsViewModel observes "bookings" where userId == uid
        │  live update arrives automatically
        ▼
BookingsFragment shows new booking in list
```

### Registering a Charging Host

```
User taps FAB on Hosts tab
        │
NavController.navigate(hostRegistrationFragment)
        │
HostRegistrationFragment (form)
        │  user fills name, address, lat/lng, price, connector
        │  taps Save
        ▼
HostRegistrationViewModel.saveHost()
        │  validates → builds ChargingHost(ownerId=uid)
        ▼
FirestoreRepository.saveHost()       [suspend + runCatching]
        │  Firestore write → new doc in "charging_hosts"
        ▼
emits UiEvent.Saved
        │
HostRegistrationFragment.popBackStack()   → returns to Hosts tab
        │
HostsViewModel observes "charging_hosts" where ownerId == uid
        │  new host appears in My Hosts list automatically
        ▼
MapViewModel observes all "charging_hosts"
        │  new pin appears on the map automatically
```

---

## Firestore Database Design

### Collection: `charging_hosts`

```json
{
  "name": "Green Lane Host",
  "address": "12 Green Lane, Grama Village",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "pricePerKwh": 12.0,
  "connectorType": "Type 2",
  "ownerId": "firebase-uid-of-owner",
  "createdAt": 1747087200000
}
```

### Collection: `bookings`

```json
{
  "hostId": "firestore-doc-id-of-host",
  "hostName": "Green Lane Host",
  "userId": "firebase-uid-of-driver",
  "startEpochMs": 1747090800000,
  "endEpochMs": 1747097600000,
  "estimatedEnergyKwh": 15.0,
  "status": "PENDING",
  "createdAt": 1747087500000
}
```

> `hostName` is **denormalised** (duplicated from the host document). This avoids a second Firestore read when displaying bookings and keeps costs low.

---

## Security Rules

File: `firestore.rules`

```
charging_hosts:
  read   → anyone (public discovery, no auth needed)
  create → authenticated users only; ownerId must equal request.auth.uid
  update/delete → authenticated user whose UID matches ownerId

bookings:
  read   → authenticated user whose UID matches userId
  create → authenticated user; userId must equal request.auth.uid
  update/delete → authenticated user whose UID matches userId
```

These rules enforce that:
- EV drivers cannot edit each other's bookings.
- A host can only be modified by the person who created it.
- Anyone can read charging hosts (so the map works even without auth).

Deploy to your Firebase project:

```bash
firebase deploy --only firestore:rules
```

---

## Local Setup

### Prerequisites

- Android Studio Ladybug (2024.x) or newer
- JDK 17
- A Firebase project with **Firestore** and **Anonymous Authentication** enabled
- A Google Cloud project with **Maps SDK for Android** enabled

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/rajkr004/EVGramaCharge.git
cd EVGramaCharge

# 2. Add your Firebase config
# Download google-services.json from Firebase Console
# (Project Settings → Your apps → Android app → google-services.json)
cp ~/Downloads/google-services.json app/google-services.json

# 3. Add your Maps API key
echo "MAPS_API_KEY=YOUR_ANDROID_MAPS_KEY_HERE" >> local.properties

# 4. Deploy Firestore security rules
firebase deploy --only firestore:rules

# 5. Open in Android Studio and Run
./gradlew :app:assembleDebug
```

> `local.properties` and `google-services.json` are both in `.gitignore` — they are never committed.

---

## Adding Your First Charging Host (Seed Data)

The app has no seed data. To see pins on the map immediately, either:

**Option A — Use the app:** open the Hosts tab → tap the FAB → fill in a name, address, and real GPS co-ordinates → save.

**Option B — Firestore Console:** Go to your Firebase Console → Firestore → add a document to `charging_hosts` with the fields described in the [Database Design](#firestore-database-design) section.

---

## Roadmap

- [ ] Email / password and Google Sign-In (replace anonymous auth)
- [ ] Host confirmation / rejection of booking requests
- [ ] Push notifications (FCM) when a booking status changes
- [ ] GeoFirestore radius search — "show only hosts within 10 km"
- [ ] Directions from current location to selected host
- [ ] Booking history for hosts (who booked, when)
- [ ] Admin dashboard for host management
- [ ] Unit tests for `BatteryCalculatorViewModel` and `FirestoreRepository`
- [ ] CI pipeline (GitHub Actions) for lint + test on every PR

---

## License

MIT License — see `LICENSE` for details.
