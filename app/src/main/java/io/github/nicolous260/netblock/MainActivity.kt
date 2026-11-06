package io.github.nicolous260.netblock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    /** Bumped when the user returns to the app, so the installed-app list is refreshed. */
    private var reloadCount by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The stored flag can be stale if the process was killed or the app was force-stopped.
        if (Prefs.isBlocking(this) && !BlockerVpnService.running) {
            Prefs.setBlocking(this, false)
        }

        setContent {
            NetBlockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Screen()
                }
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        reloadCount++
    }

    /** Runs off the main thread: querying and rasterising icons for every app is slow. */
    private fun loadApps(): List<AppItem> {
        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(launcher, 0)
            .filter { it.activityInfo.packageName != packageName }
            // An app can expose several launcher activities; the list is keyed by package.
            .distinctBy { it.activityInfo.packageName }
            .map {
                AppItem(
                    label = it.loadLabel(pm).toString(),
                    pkg = it.activityInfo.packageName,
                    icon = it.loadIcon(pm).toListIcon()
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Screen() {
        val context = this
        var blocking by remember { mutableStateOf(Prefs.isBlocking(context)) }
        var blockedApps by remember { mutableStateOf(Prefs.blocked(context)) }
        var apps by remember { mutableStateOf<List<AppItem>?>(null) }
        var searchQuery by remember { mutableStateOf("") }

        // The service owns the real state; keep the UI in step with whatever it writes.
        DisposableEffect(Unit) {
            val stopObserving = Prefs.observe(context) {
                blocking = Prefs.isBlocking(context)
                blockedApps = Prefs.blocked(context)
            }
            onDispose(stopObserving)
        }

        LaunchedEffect(reloadCount) {
            apps = withContext(Dispatchers.Default) { loadApps() }
        }

        // Starting the service also rebuilds the tunnel from the current list.
        fun startBlocking() {
            startForegroundService(Intent(context, BlockerVpnService::class.java))
        }

        fun stopBlocking() {
            startService(
                Intent(context, BlockerVpnService::class.java)
                    .setAction(BlockerVpnService.ACTION_STOP)
            )
        }

        val vpnPermission = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { if (it.resultCode == RESULT_OK) startBlocking() }

        fun requestVpn() {
            val prepare = VpnService.prepare(context)
            if (prepare != null) vpnPermission.launch(prepare) else startBlocking()
        }

        // Blocking works without the notification permission, so continue either way.
        val notificationPermission = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { requestVpn() }

        fun turnOn() {
            if (blockedApps.isEmpty()) {
                Toast.makeText(context, "Select at least one app to block first", Toast.LENGTH_SHORT).show()
                return
            }
            val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (needsNotificationPermission) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                requestVpn()
            }
        }

        fun setBlocked(pkg: String, block: Boolean) {
            Prefs.setBlocked(context, pkg, block)
            // The service rebuilds the tunnel, or stops itself if nothing is left to block.
            if (blocking) startBlocking()
        }

        val filteredApps = remember(apps, searchQuery) {
            val all = apps.orEmpty()
            if (searchQuery.isBlank()) all
            else all.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                        it.pkg.contains(searchQuery, ignoreCase = true)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("NetBlock", fontWeight = FontWeight.Medium) },
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
                        val textColor = if (blocking)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (blocking) "Protection Active" else "Protection Disabled",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (blocking)
                                    if (blockedApps.size == 1) "1 app blocked" else "${blockedApps.size} apps blocked"
                                else
                                    "Turn on to block selected apps",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.8f)
                            )
                        }
                        Switch(
                            checked = blocking,
                            onCheckedChange = { on -> if (on) turnOn() else stopBlocking() }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )

                Spacer(Modifier.height(16.dp))

                if (apps == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Text(
                        text = "Installed Apps (${filteredApps.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredApps, key = { it.pkg }) { app ->
                            AppRow(
                                app = app,
                                isBlocked = app.pkg in blockedApps,
                                onToggle = { block -> setBlocked(app.pkg, block) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val LIST_ICON_PX = 128

/**
 * Rasterise at list size instead of the icon's full intrinsic size.
 *
 * An adaptive icon is drawn on a 108dp canvas but a launcher only shows the middle 72dp, so drawn
 * naively the art ends up small inside its tile. Draw it oversized so the visible part fills it.
 */
private fun Drawable.toListIcon(): ImageBitmap {
    val bitmap = Bitmap.createBitmap(LIST_ICON_PX, LIST_ICON_PX, Bitmap.Config.ARGB_8888)
    val bleed = if (this is AdaptiveIconDrawable) LIST_ICON_PX * 18 / 72 else 0
    setBounds(-bleed, -bleed, LIST_ICON_PX + bleed, LIST_ICON_PX + bleed)
    draw(Canvas(bitmap))
    return bitmap.asImageBitmap()
}
