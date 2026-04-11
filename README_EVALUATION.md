# Evaluation Page Integration README

## 1. What was added

This Android project already uses Kotlin + Jetpack Compose, so the evaluation page is added directly on top of the existing single-activity structure instead of creating a new Android project or a separate demo.

The new page srequests `GET /api/v1/evaluation` from the Node.js + Express backend and displays:

- score
- level
- downloadAvg
- pingAvg
- rssiAvg
- snrAvg
- recordCount
- suggestions

## 2. Files added for evaluation

- `app/src/main/java/com/example/netgeocourier/helper/AuthTokenStore.kt`
- `app/src/main/java/com/example/netgeocourier/data/EvaluationModels.kt`
- `app/src/main/java/com/example/netgeocourier/network/AuthInterceptor.kt`
- `app/src/main/java/com/example/netgeocourier/network/ApiClient.kt`
- `app/src/main/java/com/example/netgeocourier/network/EvaluationApiService.kt`
- `app/src/main/java/com/example/netgeocourier/network/EvaluationRepository.kt`
- `app/src/main/java/com/example/netgeocourier/screen/EvaluationScreen.kt`

## 3. Files modified

- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/netgeocourier/MainActivity.kt`
- `app/src/main/java/com/example/netgeocourier/screen/NetTestScreen.kt`
- `app/src/main/java/com/example/netgeocourier/helper/FileHelper.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-en/strings.xml`

## 4. How page navigation works

The project keeps the current `MainActivity` single-activity structure.

- `MainActivity` stores a simple page state: `TEST` or `EVALUATION`
- `NetTestScreen` shows a new button: `Open Network Evaluation`
- Clicking the button switches the current page to `EvaluationScreen`
- `EvaluationScreen` has a `Back` button to return to the original test page

This is intentionally simpler than introducing a full navigation framework, so it is easier for student projects to understand and maintain.

## 5. Retrofit request flow

Request path:

1. `EvaluationScreen` calls `EvaluationRepository`
2. `EvaluationRepository` calls `EvaluationApiService`
3. `ApiClient` builds Retrofit + OkHttp
4. `AuthInterceptor` reads the saved token from `AuthTokenStore`
5. The interceptor adds `Authorization: Bearer <token>` automatically
6. The backend returns the evaluation JSON
7. The Compose page renders the returned data

## 6. Token storage and carrying method

### Save token after login

The Android project currently does not contain a login page implementation, so this integration does not rewrite login.

After your existing login request succeeds, save `data.accessToken` like this:

```kotlin
AuthTokenStore.saveAccessToken(context, loginResponse.data.accessToken)
```

`AuthTokenStore` stores the token in:

- SharedPreferences name: `auth_prefs`
- key: `access_token`

### Read token when requesting evaluation

`AuthInterceptor` automatically reads the token every time Retrofit sends a request:

```kotlin
val token = AuthTokenStore.getAccessToken(context)
```

If a token exists, it adds:

```http
Authorization: Bearer <accessToken>
```

If no token exists, the evaluation page shows a clear prompt telling the user to save the token first.

## 7. Backend connection configuration

The Retrofit base URL comes from `BuildConfig.API_BASE_URL`.

It is read from `local.properties`:

```properties
API_BASE_URL=http://10.0.2.2:3000/
```

If `API_BASE_URL` is not configured, the app falls back to:

```properties
http://10.0.2.2:3000/
```

Notes:

- Android emulator should use `10.0.2.2`
- Real devices should use your computer's LAN IP, for example `http://192.168.1.20:3000/`
- `AndroidManifest.xml` now enables cleartext traffic for local HTTP debugging

## 8. How to run the backend

The backend folder inside this workspace is:

- `cmapusnet-backend/campus-net-backend`

Typical startup steps:

```powershell
cd E:\projectstu_zly\NetGeoCourier2_clean\cmapusnet-backend\campus-net-backend
npm.cmd install
npm.cmd run dev
```

The backend default address is usually:

```text
http://0.0.0.0:3000
```

For Android emulator access, use:

```text
http://10.0.2.2:3000
```

## 9. Evaluation API definition

Request:

```http
GET /api/v1/evaluation
Authorization: Bearer <accessToken>
```

Example response:

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "score": 78,
    "level": "good",
    "suggestions": [
      "Overall network status is stable. Keep monitoring during peak campus hours."
    ],
    "metrics": {
      "downloadAvg": 82.5,
      "pingAvg": 28,
      "rssiAvg": -61,
      "snrAvg": 24
    },
    "recordCount": 5
  }
}
```

## 10. Why there is no XML layout file

This project is already based on Jetpack Compose.

To keep the existing project structure consistent, the evaluation page is implemented in:

- `app/src/main/java/com/example/netgeocourier/screen/EvaluationScreen.kt`

So there is no new XML layout file. The Compose file itself is the page UI implementation.

## 11. Core implementation logic

### Android side

- `MainActivity` controls which page is currently visible
- `NetTestScreen` remains the existing test page and only adds an entry button to the new evaluation page
- `EvaluationScreen` is responsible for rendering UI and triggering refresh
- `EvaluationRepository` handles request/result conversion
- `EvaluationApiService` defines the backend API
- `AuthInterceptor` injects the Bearer token
- `AuthTokenStore` manages token persistence

### Backend side

- the backend verifies the Bearer token
- the backend finds the current user from the token
- the backend calculates averages and score
- the backend returns a normalized JSON object
- the Android app displays the returned fields directly

This makes the front/back connection very clear:

- login produces `accessToken`
- token is saved locally
- evaluation page sends token to backend
- backend returns evaluation result
- Android renders result cards and suggestion list

## 12. Suggested integration point in your existing login flow

If your current login module already receives a response similar to:

```json
{
  "data": {
    "accessToken": "xxx"
  }
}
```

then in the login success callback, add only one line:

```kotlin
AuthTokenStore.saveAccessToken(context, response.data.accessToken)
```

That is enough for the new evaluation page to work.

## 13. Notes for student contest projects

This implementation intentionally stays simple:

- no new Android project
- no independent demo module
- no login module rewrite
- no complicated architecture changes
- minimal new classes focused only on evaluation
- page style is simple, clean, and easy to present in a competition demo

## 14. Validation note

I also corrected several malformed source/resource files already present in the Android project so the modified files are syntactically coherent. However, local Gradle verification is still blocked in this environment by the machine's Java/Gradle setup error (`java.lang.IllegalArgumentException: 25.0.2`), so please run the project in Android Studio after syncing Gradle and confirm the local JDK configuration.
