# CARRY — Native Android Application (Pure Java)

The official native Android application for **CARRY** (Peer-to-Peer KUET Campus Delivery & Carpooling Platform), engineered with **pure Java**, Google Material3, Retrofit 2, and Glide.

---

## 📱 Features & Highlights

1. **Campus Shop & Catalog**:
   - Live product browsing across categories (**🍔 Food & Snacks**, **🔌 Electronics**, **📚 Stationery & Others**).
   - Real-time search by keyword and category filters.
   - Quantity adjustment selector directly on product cards (`- [qty] +`).
   - **🛒 Add to Cart** for multi-item batch ordering.
   - **⚡ Express Order Now**: Full-width button on each card that opens an isolated checkout dialog for that specific product without adding to or clearing your existing cart.

2. **Upfront MFS Payment Portal (bKash / Nagad)**:
   - Integrated into both Express Order Now, Cart Bottom Sheet, and Custom Request screens.
   - Automatic live calculation: `Product Cost + Base Delivery Fee (৳30.00) + 1.39% MFS Cash-out Fee = Total to Send`.
   - Brand-matched bKash (Pink) and Nagad (Orange) selectors.
   - Campus account banner: `01700000000` (tap to copy to clipboard).
   - Sender Phone Number and TrxID validation.

3. **Multi-Item Shopping Cart**:
   - Header badge displaying real-time item count.
   - Interactive Bottom Sheet showing item list with live +/- quantity adjustments and item deletion.
   - Single-tap batch checkout for multiple campus items.

4. **Custom Manual Requests**:
   - Post any item needed from Khulna city to KUET.
   - Specify Item Name, Category, Quantity, Budget, Khulna Pickup Zone (New Market, Sonadanga, Gollamari, etc.), and special instructions.

5. **Order Tracking & Lifecycle Management**:
   - View your orders with color-coded status badges: `REQUESTED`, `ACCEPTED`, `COLLECTED`, `RETURNING`, `READY_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`.
   - Detailed step-by-step timeline of delivery progression with timestamps and notes.
   - **Need-More Additional Payment Support**: If the city carrier requests additional funds (due to higher store pricing), pay directly via bKash/Nagad and submit TrxID to unblock delivery.
   - Safe cancellation for pending requests.

6. **Carrier / Delivery Hub (Earn Delivery Money)**:
   - Post a trip when heading from KUET to Khulna city (destination area, departure time, return time, carry capacity).
   - Advance trip status (`In City` -> `Returning` -> `Done`).
   - Browse open campus requests in Khulna and accept them with 1-tap to earn delivery charges.

7. **KUET Student Profile & Server Environment Switcher**:
   - Student info, ID, Department, and verified KUET badge.
   - **Live Backend Switcher**:
     - `Local Emulator` (`http://10.0.2.2:8080/`)
     - `Cloud Backend` (`https://carry-backend-hctl.onrender.com/`)
   - Secure Logout.

---

## 🚀 How to Open and Run in Android Studio

1. **Launch Android Studio** (e.g. from `/Applications/Android Studio.app`).
2. Click **Open** (or **File -> Open...**).
3. Select the folder:
   ```
   /Users/farhanisraq/Downloads/CARRY/android
   ```
4. Click **OK**.
5. Android Studio will automatically recognize the Gradle project and perform the initial Gradle sync.
6. Once the sync finishes:
   - Select an Android Virtual Device (e.g. Pixel 7 / Pixel 8 running API 34).
   - Click the green **Run (▶)** button (or press `Ctrl + R` / `Shift + F10`).
7. The CARRY app will build, install, and open on the emulator!

---

## 🔑 Demo Account Credentials

You can sign in with an existing account or create a new student account:
- **Email:** `farhan@kuet.ac.bd`
- **Password:** `password123`
- *Or tap "Register here" on the login screen to register with your KUET student ID.*

---

## 🌐 Backend Connectivity Note

- **On Android Emulator:** The local Spring Boot backend running on your Mac is accessible at `http://10.0.2.2:8080/`. This is pre-configured as default.
- **On Physical Devices / Remote:** Go to the **Profile** tab inside the app and tap **Cloud Backend (Render Production)** to immediately connect to `https://carry-backend-hctl.onrender.com/`.

---

## 📁 Project Architecture

```
android/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/carry/app/
│   │   │   ├── MainActivity.java
│   │   │   ├── adapters/
│   │   │   │   ├── ProductAdapter.java
│   │   │   │   ├── CartAdapter.java
│   │   │   │   ├── RequestAdapter.java
│   │   │   │   ├── AvailableRequestAdapter.java
│   │   │   │   └── TimelineAdapter.java
│   │   │   ├── api/
│   │   │   │   ├── ApiClient.java
│   │   │   │   └── ApiService.java
│   │   │   ├── models/
│   │   │   │   ├── User.java, Product.java, ProductRequest.java, Trip.java, Payment.java
│   │   │   │   ├── LocationArea.java, ProductCategory.java, RequestStatus.java, etc.
│   │   │   │   └── DTOs (CheckoutLineDto, CreateTripRequest, AcceptRequestDto, etc.)
│   │   │   ├── ui/
│   │   │   │   ├── auth/ (LoginActivity, RegisterActivity)
│   │   │   │   ├── shop/ (ShopFragment)
│   │   │   │   ├── requests/ (NewRequestFragment, MyRequestsFragment, RequestDetailActivity)
│   │   │   │   ├── trips/ (DeliveriesFragment, PostTripActivity)
│   │   │   │   └── profile/ (ProfileFragment)
│   │   │   └── utils/
│   │   │       ├── CartManager.java
│   │   │       ├── DeliveryChargeCalculator.java
│   │   │       └── SessionManager.java
│   │   └── res/
│   │       ├── drawable/ (Vector icons, background cards, badges)
│   │       ├── layout/ (XML screens and dialogs)
│   │       ├── menu/ (bottom_nav_menu.xml)
│   │       └── values/ (colors.xml, strings.xml, themes.xml)
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── local.properties
```
