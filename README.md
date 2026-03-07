This is a Kotlin Multiplatform project targeting Desktop (JVM).

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
      folder is the appropriate location.

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

# TODOs
As we move to **Windows**, there are three specific "polish" items that 
usually come up with this `isConfigured` check:

- Reset Config Info
- Platform-Specific Defaults in `SetupScreen`
- The Database "First Run" Catch
- Offline Indicator

### 1. The "Reset" Path

If you accidentally enter the wrong Dropbox token on Windows, the app 
will save the file, `isConfigured` becomes `true`, and you'll 
be stuck on a broken `MainScreen`.

**The Fix:** Add a "Reset Configuration" button in your `MainScreen` 
settings (or even as a small text link in the `BottomAppBar`) that calls:

```kotlin
fun resetConfig() {
    ConfigManager.deleteConfig() // Deletes the file in AppData or .config
    // In your EBookApp, you'd need a way to flip 'isConfigured' back to false
}
```

However, while we can delete the configuration file, the properties are in a 
`val`, not a `var`.  This means we can't just instantiate a new one.  We will 
have to see if we can 'empty' it and restart the app, or something similar.

### 2. Platform-Specific Defaults in `SetupScreen`

When you run this on Windows for the first time, your `SetupScreen` 
should ideally "guess" the correct paths and viewer commands so you 
don't have to type them.

Inside your `SetupScreen` composable, you can use the `Platform` 
object we built:

```kotlin
// Inside SetupScreen.kt
val defaultCommand = remember { Platform.getDefaultViewerCommand() }
var viewerCommand by remember { mutableStateOf(defaultCommand) }
```

However, the current `Platform.getDefaultViewerCommand()` takes an
absolute path and generates the full command from that, instead of
providing it with a `%f` to be replaced by the file path.  This needs
to be reconciled.

### 3. The Database "First Run" Catch

On Ubuntu, your MySQL database is likely already running. On a fresh 
Windows machine, you might not have the DB set up yet.

If `isConfigured` is true but the **Database connection fails**, your 
`ViewModel` will currently just show a Snackbar. You might want to update 
your `EBookApp` logic to handle "Partial Configuration" (e.g., config 
file exists, but connection is refused):

```kotlin
// In EBookViewModel
val connectionError by mutableStateOf<String?>(null)

// In EBookApp
if (!isConfigured || viewModel.connectionError != null) {
    SetupScreen(error = viewModel.connectionError)
}
```
It appears that the `connectionError` might have to point to some similar
state in the `DatabaseManager` as well.

### The Offline Indicator
You'll want a visual indicator in your BottomAppBar (the stats bar we built earlier) 
to show that you are in "Offline Mode" and have "X Pending Changes." 
It adds a lot of confidence when you see that the app is tracking your work.
