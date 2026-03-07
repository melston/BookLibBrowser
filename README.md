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

# Building on Windows
The existing configuration is a solid foundation, but to get a functional `.msi` 
(Windows Installer), we need to address a few "Windows-specific" requirements. 
Specifically, Windows installers are much pickier about icons, licensing, and the 
toolchain required to build them.

### 1. The Wix Toolset Requirement

To generate an `.msi` on a Windows machine, you **must** have the **Wix Toolset** 
(version 3.x is standard for Compose) installed on that laptop.

* If you try to run `./gradlew packageMsi` on Linux, it will fail because the 
  packaging tools are platform-dependent.
* You can install it via [wixtoolset.org](https://wixtoolset.org/releases/) or via 
 `choco install wixtoolset` if you use Chocolatey.

---

### 2. Refined `windows` Configuration

Here is the updated block with the necessary additions for a professional Windows experience:

```kotlin
windows {
    shortcut = true
    menuGroup = "EBook Library Browser"
    // upgradeUuid is vital for updates (prevents double-installing)
    upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890" 
    
    // Windows requires an .ico file specifically. 
    // You can convert your books.png to books.ico online or via GIMP.
    iconFile.set(project.file("src/jvmMain/resources/books.ico"))

    // This puts the app in "Add/Remove Programs" correctly
    dirChooser = true 
    perUserInstall = false // Set to true if you don't have Admin rights on the work laptop
    
    // Optional: If you want a specific splash screen on Windows startup
    // bitmapEditor = project.file("src/jvmMain/resources/splash.bmp")
}
```

### 3. Handling the `modules`

You've manually listed several modules. Since you're using **MySQL (JDBC)** 
and **Gson**, Compose needs to know which parts of the JDK to bundle. A more 
"future-proof" way to do this in Compose is to let it attempt to find them, but 
keep your manual list for the "tricky" ones:

```kotlin
nativeDistributions {
    // ... existing metadata ...
    
    modules("java.instrument", "java.sql", "jdk.unsupported", "java.naming", "java.desktop")
    
    // This helps include standard library parts often missed by reflection-heavy libraries like Gson
    includeAllModules = false 
}
```

---

### 4. The Build Commands

Once you have this in your `build.gradle.kts` and you are on your Windows laptop:

1. **To test the app without installing:**
```bash
./gradlew run
```

2. **To create a "loose" folder with an .exe (fastest for testing):**
```bash
./gradlew createDistributable
```

*This will be in `build/compose/binaries/main/app`.*
3. **To generate the full .msi installer:**
```bash
./gradlew packageMsi
```

*This will be in `build/compose/binaries/main/msi`.*

---

### 5. A Quick "Work Laptop" Tip

Since it's a work laptop, you might run into **Code Signing** issues. 
Windows might show a "Windows protected your PC" (SmartScreen) warning 
because the `.msi` isn't signed with a $400/year certificate.

* To bypass this, you just click **"More Info"** -> **"Run Anyway"**.
* If your work IT policy is extremely strict, you might prefer using the 
  output of `createDistributable` (the folder with the `.exe`) and just 
  running it from a local directory rather than "installing" it.

### Summary of Tasks

* [ ] Convert `books.png` to `books.ico` and place it in resources.
* [ ] Install Wix Toolset on the Windows machine.
* [ ] Update the `windows` block in `build.gradle.kts`.

**Would you like me to show you how to add a custom "About" dialog to the UI?** 
Since you've added versioning and copyright to the Gradle file, it’s nice to 
display that information inside the app's settings so you can verify which 
version is currently running on your laptop.

# TODOs
As we move to **Windows**, there are three specific "polish" items that 
usually come up with this `isConfigured` check:

- Reset Config Info
- Platform-Specific Defaults in `SetupScreen`
- The Database "First Run" Catch

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
