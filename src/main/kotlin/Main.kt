package io.github.landon_kuehner

// For window
import javax.swing.JFrame
import javax.swing.JLabel
import java.awt.Toolkit
import java.awt.Color
import java.awt.Point
import java.awt.Font
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import com.sun.java.accessibility.util.AWTEventMonitor.addKeyListener

import kotlin.system.exitProcess

// For key listening
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeHookException
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener


// Some random functions
fun List<Boolean>.isAllTrue(): Boolean {
	for (i in this) {
		if (!i) {
			return false
		}
	}
	return true
}

fun List<Boolean>.isAllFalse(): Boolean {
	for (i in this) {
		if (i) {
			return false
		}
	}
	return true
}

val shift_replacements = mapOf(
	"`" to "~",
	"1" to "!",
	"2" to "@",
	"3" to "#",
	"4" to "$",
	"5" to "%",
	"6"  to "^",
	"7" to "&",
	"8" to "*",
	"9" to "(",
	"0" to ")",
	"-" to "_",
	"=" to "+",
	"\\" to "|",
	";" to ":",
	"'" to '"',
	"," to "<",
	"." to ">",
	"/" to "?"
)

val replacements = mapOf(
	"Comma" to ",",
	"Period" to ".",
	"Slash" to "/",
	"Semicolon" to ";",
	"Quote" to "'",
	"Open Bracket" to "{",
	"Close Bracket" to "}",
	"Back Slash" to "\\",
	"Minus" to "-",
	"Equals" to "=",
	"Back Quote" to "`",

	"Space" to "⎵",
	"Enter" to "↵",
	"Backspace" to " ⟵ "
)

// Parses key lists and produces a string for the window to display
fun genStringFromKeys(modkeys: MutableList<Boolean>, keys: MutableList<String>): String {

	if (modkeys.isEmpty() && keys.isEmpty()) {
		return ""
	}

	var finalString = ""
	if (modkeys[0]) {
		finalString += "⌘"
	}
	if (modkeys[1]) {
		finalString += "^"
	}
	if (modkeys[2]) {
		finalString += "⇧"
	}
	if (modkeys[3]) {
		finalString += "⌥"
	}
	if (finalString.isNotEmpty()) {
		finalString = finalString.split("").joinToString(" + ").substring(3)
	}
	keys.forEachIndexed { index, value ->
		if (value.matches("F\\d+".toRegex())) {
			if (index != keys.lastIndex) {
				finalString += "$value + "
			}
			else if (index != 0) {
				finalString += " + $value"
			}
			else {
				finalString += " + $value + "
			}
		}
		else {
			finalString += value
		}
	}
	return finalString
}

fun main() {
	val modKeys: MutableList<Boolean> = mutableListOf(false, false, false, false) // Meta + Ctrl + Shift + Alt (No fn support)
	val keys: MutableList<String> = mutableListOf()
	var timer: Int = 2500

	val screenSize = Toolkit.getDefaultToolkit().screenSize
	val screenHeight = screenSize.height

	val frame = JFrame("Key Caster")
	frame.setSize(screenSize.width, (screenHeight / 5))
	frame.location = Point(0, screenHeight - (screenHeight / 5))

	frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

	frame.isUndecorated = true
	frame.isAlwaysOnTop = true
	frame.background = Color(42, 42, 42, 200)

	// Adds the display for the key presses
	val keyDisplay = JLabel("", JLabel.CENTER)
	keyDisplay.font = Font(Font.MONOSPACED, Font.PLAIN, 35)
	keyDisplay.foreground = Color.WHITE
	frame.add(keyDisplay)

	addKeyListener(object : KeyAdapter() {
		override fun keyPressed(e: KeyEvent) {
			if (e.keyCode == KeyEvent.VK_ESCAPE) {
				println("Escape key pressed")
				// Close the JFrame
				exitProcess(0)
			}
		}
	})

	frame.isVisible = true

	try {
		// Register the native hook.
		GlobalScreen.registerNativeHook()

		// Define the listener.
		val listener = object : NativeKeyListener {
			override fun nativeKeyTyped(event: NativeKeyEvent?) {}

			override fun nativeKeyReleased(event: NativeKeyEvent?) {
				val keyText = NativeKeyEvent.getKeyText(event?.keyCode ?: 0)
				println("User unpressed $keyText")
				if (keyText in listOf("¥", "Ctrl", "Shift", "Alt")) {
					when(keyText) {
						"¥" -> {
							modKeys[0] = false
						}
						"Ctrl" -> {
							modKeys[1] = false
						}
						"Shift" -> { // Doesn't seem to undetect
							modKeys[2] = false
						}
						"Alt" -> {
							modKeys[3] = false
						}
					}
					if (modKeys.isAllFalse()) {
						Thread.sleep(1000)
						keys.clear()
					}
					val generatedString: String = genStringFromKeys(modKeys, keys)
					keyDisplay.text = generatedString
					timer = 2500
				}
			}

			override fun nativeKeyPressed(event: NativeKeyEvent?) {
				var keyText = NativeKeyEvent.getKeyText(event?.keyCode ?: 0)
				println("User pressed $keyText")
				if (keyText in listOf("¥", "Ctrl", "Shift", "Alt")) {
					when(keyText) {
						"¥" -> {
							modKeys[0] = true
						}
						"Ctrl" -> {
							modKeys[1] = true
						}
						"Shift" -> {
							modKeys[2] = true
						}
						"Alt" -> {
							modKeys[3] = true
						}
					}
				}
				else {
					if (replacements.containsKey(keyText)) {
						keyText = replacements[keyText]
					}

					if (modKeys[2] && shift_replacements.containsKey(keyText)) {
						keyText = shift_replacements[keyText].toString()
					}

					keys += keyText
				}

				val generatedString: String = genStringFromKeys(modKeys, keys)
				println("Generated string: $generatedString")
				keyDisplay.text = generatedString
				timer = 2500
			}
		}

		// Add the listener.
		GlobalScreen.addNativeKeyListener(listener)

		while (true) {
			Thread.sleep(50)
			timer -= 50
			if (timer <= 0) {
				if (modKeys.isAllFalse()) {
					keys.clear()
					val generatedString: String = genStringFromKeys(modKeys, keys)
					keyDisplay.text = generatedString
				}
				timer = 2500
			}
		}
	}
	catch (err: NativeHookException) {
		println("There was a problem registering the native hook.")
		err.printStackTrace()
	}

}
