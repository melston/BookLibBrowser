package org.elsoft.bkdb

import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import java.awt.GraphicsEnvironment
import java.io.File
import java.util.Properties

fun main() = application {
    // 1. Load the saved state from a local file
    val propsFile = File("window.properties")
    val props = Properties()
    if (propsFile.exists()) propsFile.inputStream().use { props.load(it) }

    // Default to a sensible size if no record exists
    val savedWidth = props.getProperty("width")?.toIntOrNull() ?: 1200
    val savedHeight = props.getProperty("height")?.toIntOrNull() ?: 800
    val x = props.getProperty("x")?.toIntOrNull()
    val y = props.getProperty("y")?.toIntOrNull()

    // 2. Validate against current hardware
    val isVisible = if (x != null && y != null) {
        val screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .screenDevices
            .map { it.defaultConfiguration.bounds }

        // Check if the saved top-left corner exists within any current monitor's bounds
        screenBounds.any { it.contains(x, y) }
    } else false

    // 3. Initialize the WindowState
    val windowState = rememberWindowState(
        size = DpSize(savedWidth.dp, savedHeight.dp),
        position = if (isVisible) {
            WindowPosition(x!!.dp, y!!.dp)
        } else {
            WindowPosition(Alignment.Center)
        }
    )

    Window(
        onCloseRequest = {
            // 4. Capture and Save
            saveWindowProperties(props, propsFile, windowState)
            exitApplication()
        },
        state = windowState,
        title = "EBook Manager"
    ) {
        EBookApp()
    }
}

private fun saveWindowProperties(props: Properties, file: File, state: WindowState) {
    props.setProperty("width", state.size.width.value.toInt().toString())
    props.setProperty("height", state.size.height.value.toInt().toString())

    val pos = state.position
    if (pos is WindowPosition.Absolute) {
        props.setProperty("x", pos.x.value.toInt().toString())
        props.setProperty("y", pos.y.value.toInt().toString())
    }
    file.outputStream().use { props.store(it, null) }
}
