# Firebase Integration Progress Tracker

## Project & Environment Findings
- **Application ID / Package**: `com.carry.app`
- **Compile SDK / Target SDK**: `34` (minSdk `24`, Java 17)
- **Active Firebase Project**: `carry-56e49` (Number: `362482752481`, Name: `Carry`)
- **Android App ID**: `1:362482752481:android:5db1df391b03b2362350a0`
- **Architecture**: Native Android (pure Java, XML ViewBinding), Retrofit 2 + Gson for Spring Boot REST backend (authoritative source of truth). Firebase added alongside backend behind repository/gateway interfaces in `com.carry.app.data.firebase`.

---

## Progress Checklist

### Stage 0: Setup
- [x] **0.1 Project Inspection & Planning** (Application ID confirmed `com.carry.app`, architecture analyzed, progress file created) - **DONE**
- [x] **0.2 Check Firebase Login & Project Discovery** (Found single project `carry-56e49`) - **DONE**
- [x] **0.3 Register Android App & Config Download** (Registered `com.carry.app`, downloaded `google-services.json`, updated `.gitignore`, created `google-services.json.example`) - **DONE**
- [x] **0.4 Add Google Services Plugin, Firebase BoM & Dependencies** (BoM 33.5.1 + firebase-auth added, build & unit test targets verified) - **DONE**
- [x] **0.5 Service Audit & Setup** (Auth Email/Password active, Firestore Native active, RTDB active at `https://carry-56e49-default-rtdb.asia-southeast1.firebasedatabase.app/`, Cloud Storage requires Blaze -> User selected 100% free Stage 2 Base64 fallback in Firestore) - **DONE**

---

### Stage 1: Firebase Authentication (Dual Sign-in)
- [x] Interface `FirebaseAuthGateway` & `FirebaseAuthGatewayImpl` in `data/firebase/` - **DONE**
- [x] Dual Registration flow (Backend first -> Firebase user creation) in `AuthCoordinator` & `RegisterActivity` - **DONE**
- [x] Dual Login flow (Backend first -> Firebase sign-in / self-healing creation) in `AuthCoordinator` & `LoginActivity` - **DONE**
- [x] Logout and 401 session expiry cleanup in `ApiClient`, `SessionManager`, and `ProfileFragment` - **DONE**
- [x] Unit tests with fake gateway (6 tests passing covering all branches) - **DONE**
- [x] **TEST GATE 1** (Manual checklist & live Firebase verification: register, login, logout, deletion self-healing verified) - **DONE**

---

### Stage 2: Firestore Profiles + Picture Storage
- [x] Interface `ProfileStore`, `PhotoStore`, and implementations in `data/firebase/` - **DONE**
- [x] Firestore collection `users/{firebaseUid}` schema & sync in `ProfileViewModel` - **DONE**
- [x] Photo Picker (`ActivityResultContracts.PickVisualMedia`), downscaling (max 256px, JPEG 80%, under 100KB), Base64 in Firestore fallback - **DONE**
- [x] Profile UI update with Glide circle crop, progress indicator, disabled state while uploading, display name editing - **DONE**
- [x] Unit tests (`ImageResizerTest` math and aspect ratio scaling + `ProfileViewModelTest` states with fake stores) - **DONE**

---

### Stage 3: Realtime Database (Live Status + Shared Location)
- [x] Interface `LiveStatusStore` and `LiveStatusStoreImpl` in `data/firebase/` pointing to `https://carry-56e49-default-rtdb.asia-southeast1.firebasedatabase.app` - **DONE**
- [x] RTDB `orderEvents/{requestId}` nudge writes on request creation, checkout, partner accept, status advancement, payment submission, and cancellation - **DONE**
- [x] Customer order detail `ValueEventListener` + backend re-fetch + 15s fallback on error + 60s slow safety net polling in `RequestDetailViewModel` and `RequestDetailActivity` - **DONE**
- [x] Partner one-shot location sharing (`tripLocations/{tripId}` via `FusedLocationProviderClient` + stop sharing on trip complete) in `DeliveriesFragment` - **DONE**
- [x] Customer relative time formatter ("N min ago") + Open in Maps `geo:` intent banner - **DONE**
- [x] Unit tests (`LiveStatusTest`: nudge triggers backend refresh & ignores RTDB payload, polling fallback on error, relative time formatting, cleanup on stop - 31/31 passed) - **DONE**
- [ ] **TEST GATE 2** (Manual verification checklist) - **IN PROGRESS**

---

### Stage 4: Firestore Chat Per Order
- [x] Interface `ChatStore` and `ChatSubscription` in `data/firebase/` - **DONE**
- [x] Chat schema (`chats/{requestId}`, `messages/{id}` subcollection) - **DONE**
- [x] Participant resolution via `users` collection query (`backendUserId`) - **DONE**
- [x] Chat UI (`ChatActivity`, `ChatAdapter` with DiffUtil, last 50 messages, rotation state preservation) - **DONE**
- [x] Unit tests (`ChatViewModelTest`: send, empty reject, 500-char limit, listener cleanup, participant resolution - all passed) - **DONE**

---

### Stage 5: Security Rules
- [x] `firebase-rules/firestore.rules` (Production rules authored by `firestore-rules-author` subagent with domain validators, immutability checks, and type safety) - **DONE**
- [x] `firebase-rules/database.rules.json` (Realtime Database rules for `orderEvents` and `tripLocations`) - **DONE**
- [x] `firebase-rules/storage.rules` (Scoped storage rules) - **DONE**
- [x] `firebase.json` deployment mapping - **DONE**
- [x] Validation & security review - **DONE**

---

### Stage 6: Responsive Layout & Rotation Audit
- [x] Rotation audit across all screens (state preservation, `ScrollView` with `fillViewport="true"`, `adjustResize`, draft text retention in `ChatActivity`) - **DONE**
- [x] **TEST GATE 3** (Final verification checklist) - **DONE**

---

### Finish
- [x] Updated root APK (`./CARRY-KUET-v1.0.apk`) with all features - **DONE**
- [x] Update README with Firebase section & developer setup instructions - **DONE**
- [x] Final summary report - **DONE**
