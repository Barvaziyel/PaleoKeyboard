package com.example.paleokeyboard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.paleokeyboard.ui.theme.PaleoKeyboardTheme

/** Gets the keyboard turned on and gives you somewhere to try it. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaleoKeyboardTheme {
                SetupScreen()
            }
        }
    }
}

@Composable
fun SetupScreen() {
    val context = LocalContext.current
    // Settings and the keyboard picker take focus away from us; check again when it's back.
    val focused = LocalWindowInfo.current.isWindowFocused
    val enabled = remember(focused) { context.isKeyboardEnabled() }
    val selected = remember(focused) { context.isKeyboardSelected() }
    var text by remember { mutableStateOf("") }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column {
                Text("𐤀𐤁𐤂𐤃", style = MaterialTheme.typography.displaySmall)
                Text("Phoenician Keyboard", style = MaterialTheme.typography.headlineSmall)
            }

            Step(
                title = "1. Turn it on",
                done = enabled,
                action = "Open settings",
            ) {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }

            Step(
                title = "2. Switch to it",
                done = selected,
                action = "Choose keyboard",
                actionEnabled = enabled,
            ) {
                context.getSystemService(InputMethodManager::class.java).showInputMethodPicker()
            }

            SpaceBarSetting()

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Try it here") },
                // Follow the text: Phoenician lines start on the right.
                textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Content),
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                "Long-press the space bar to switch keyboards.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Step(
    title: String,
    done: Boolean,
    action: String,
    actionEnabled: Boolean = true,
    onAction: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (done) {
            Text("✓ Done", color = MaterialTheme.colorScheme.primary)
        } else {
            Button(onClick = onAction, enabled = actionEnabled) { Text(action) }
        }
    }
}

/** On: the space bar types the word divider. Off: a normal space. */
@Composable
private fun SpaceBarSetting() {
    val context = LocalContext.current
    var divider by remember { mutableStateOf(KeyboardPrefs.spaceIsDivider(context)) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.toggleable(value = divider, role = Role.Switch) {
            divider = it
            KeyboardPrefs.setSpaceIsDivider(context, it)
        },
    ) {
        Column(Modifier.weight(1f)) {
            Text("Word divider on the space bar", style = MaterialTheme.typography.titleMedium)
            Text(
                "Space types $WORD_DIVIDER, the small dot Phoenician puts between words. " +
                    "Turn off for a normal space.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = divider, onCheckedChange = null)
    }
}

private fun Context.isKeyboardEnabled(): Boolean =
    getSystemService(InputMethodManager::class.java)
        .enabledInputMethodList.any { it.packageName == packageName }

private fun Context.isKeyboardSelected(): Boolean {
    val current = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        getSystemService(InputMethodManager::class.java).currentInputMethodInfo?.packageName
    } else {
        Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.let { ComponentName.unflattenFromString(it)?.packageName }
    }
    return current == packageName
}
