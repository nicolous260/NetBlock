package com.example.netblock

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class AppItem(
    val label: String,
    val pkg: String,
    val icon: Drawable?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, 0)
            .map {
                AppItem(
                    label = it.loadLabel(pm).toString(),
                    pkg = it.activityInfo.packageName,
                    icon = it.loadIcon(pm)
                )
            }
            .filter { it.pkg != packageName }
            .sortedBy { it.label.lowercase() }

        setContent {
            NetBlockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Screen(apps)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Screen(apps: List<AppItem>) {
        var blocking by remember { mutableStateOf(Prefs.isBlocking(this)) }
        var blockedApps by remember { mutableStateOf(Prefs.blocked(this)) }
        var searchQuery by remember { mutableStateOf("") }

        fun startBlocking() {
            startForegroundService(Intent(this, BlockerVpnService::class.java))
            blocking = true
        }

        fun stopBlocking() {
            startService(
                Intent(this, BlockerVpnService::class.java)
                    .setAction(BlockerVpnService.ACTION_STOP)
            )
            blocking = false
        }

        val vpnPermission = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { if (it.resultCode == RESULT_OK) startBlocking() }

        fun turnOn() {
            val prepare = VpnService.prepare(this)
            if (prepare != null) vpnPermission.launch(prepare) else startBlocking()
        }

        val filteredApps = remember(apps, searchQuery) {
            if (searchQuery.isBlank()) apps
            else apps.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                        it.pkg.contains(searchQuery, ignoreCase = true)
            }
        }

        Scaffold(
            topBar = {
        TopAppBar(
            title = {
                Text("NetBlock", fontWeight = FontWeight.Bold)
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Hero Status Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (blocking)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (blocking) "Protection Active" else "Protection Disabled",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (blocking)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (blocking)
                                    "${blockedApps.size} app(s) blocked from internet"
                                else
                                    "Turn on to block selected apps",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (blocking)
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Switch(
                            checked = blocking,
                            onCheckedChange = { on ->
                                if (on) turnOn() else stopBlocking()
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )

                Spacer(Modifier.height(16.dp))

                // List Header
                Text(
                    text = "Installed Apps (${filteredApps.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                // App List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredApps, key = { it.pkg }) { app ->
                        val isBlocked = app.pkg in blockedApps
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newBlocked = !isBlocked
                                    Prefs.setBlocked(this@MainActivity, app.pkg, newBlocked)
                                    blockedApps = Prefs.blocked(this@MainActivity)
                                    if (blocking) {
                                        if (blockedApps.isEmpty()) {
                                            stopBlocking()
                                        } else {
                                            startBlocking()
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isBlocked)
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                else
                                    MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // App Icon
                                val bitmap = remember(app.icon) { app.icon?.toImageBitmap() }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = app.label.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = if (isBlocked) "Internet blocked" else "Internet allowed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isBlocked)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                Switch(
                                    checked = isBlocked,
                                    onCheckedChange = { block ->
                                        Prefs.setBlocked(this@MainActivity, app.pkg, block)
                                        blockedApps = Prefs.blocked(this@MainActivity)
                                        if (blocking) {
                                            if (blockedApps.isEmpty()) {
                                                stopBlocking()
                                            } else {
                                                startBlocking()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Drawable.toImageBitmap(): ImageBitmap {
    val width = if (intrinsicWidth > 0) intrinsicWidth else 96
    val height = if (intrinsicHeight > 0) intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}

@Composable
fun NetBlockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF9ECAFE),
            onPrimary = Color(0xFF00325B),
            primaryContainer = Color(0xFF00497D),
            onPrimaryContainer = Color(0xFFD1E4FF),
            secondary = Color(0xFFBBC7DB),
            onSecondary = Color(0xFF253140),
            secondaryContainer = Color(0xFF3B4858),
            onSecondaryContainer = Color(0xFFD7E3F8),
            background = Color(0xFF111318),
            onBackground = Color(0xFFE2E2E6),
            surface = Color(0xFF111318),
            onSurface = Color(0xFFE2E2E6),
            surfaceContainerLow = Color(0xFF1A1C22),
            surfaceVariant = Color(0xFF42474E),
            onSurfaceVariant = Color(0xFFC2C7CF),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF0061A4),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD1E4FF),
            onPrimaryContainer = Color(0xFF001D36),
            secondary = Color(0xFF535F70),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFD7E3F8),
            onSecondaryContainer = Color(0xFF0F1D2A),
            background = Color(0xFFFDFCFF),
            onBackground = Color(0xFF1A1C1E),
            surface = Color(0xFFFDFCFF),
            onSurface = Color(0xFF1A1C1E),
            surfaceContainerLow = Color(0xFFF0F4F9),
            surfaceVariant = Color(0xFFDFE2EB),
            onSurfaceVariant = Color(0xFF42474E),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
