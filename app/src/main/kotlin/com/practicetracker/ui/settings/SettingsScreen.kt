package com.practicetracker.ui.settings

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.practicetracker.data.datastore.AppSettings
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onDeletedNavigateToOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // Observe delete event
    LaunchedEffect(Unit) {
        viewModel.deleteEvent.collect {
            onDeletedNavigateToOnboarding()
        }
    }

    // Profile form state
    var nameField by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var instrumentField by remember(profile.instrument) { mutableStateOf(profile.instrument) }
    var skillLevel by remember(profile.skillLevel) { mutableStateOf(profile.skillLevel) }
    var teacherField by remember(profile.teacherName) { mutableStateOf(profile.teacherName) }
    var avatarUriField by remember(profile.avatarUri) { mutableStateOf(profile.avatarUri) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Camera URI
    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { avatarUriField = it.toString() }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { avatarUriField = it.toString() }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.updateSettings(settings.copy(remindersEnabled = true))
        }
    }

    fun launchCamera() {
        val photoFile = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ---- Profile Section ----
            SettingsSectionHeader("Profile")

            // Avatar picker
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUriField.isNotBlank()) {
                    AsyncImage(
                        model = avatarUriField,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    if (nameField.isNotBlank()) {
                        Text(
                            text = nameField.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = "Pick avatar",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { launchCamera() }) { Text("Camera") }
                TextButton(onClick = { galleryLauncher.launch("image/*") }) { Text("Gallery") }
            }

            OutlinedTextField(
                value = nameField,
                onValueChange = { nameField = it },
                label = { Text("Display name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = instrumentField,
                onValueChange = { instrumentField = it },
                label = { Text("Instrument") },
                modifier = Modifier.fillMaxWidth()
            )

            val skillLevels = listOf("Beginner", "Intermediate", "Advanced", "Professional")
            var skillDropdownExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = skillDropdownExpanded,
                onExpandedChange = { skillDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = skillLevel.ifBlank { "" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Skill level") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = skillDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = skillDropdownExpanded,
                    onDismissRequest = { skillDropdownExpanded = false }
                ) {
                    skillLevels.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level) },
                            onClick = {
                                skillLevel = level
                                skillDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = teacherField,
                onValueChange = { teacherField = it },
                label = { Text("Teacher name") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.updateProfile(
                        name = nameField,
                        instrument = instrumentField,
                        skillLevel = skillLevel,
                        teacherName = teacherField,
                        avatarUri = avatarUriField
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Profile")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ---- Notifications Section ----
            SettingsSectionHeader("Notifications")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Practice reminders")
                Switch(
                    checked = settings.remindersEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            viewModel.updateSettings(settings.copy(remindersEnabled = false))
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val permission = Manifest.permission.POST_NOTIFICATIONS
                            val granted = ContextCompat.checkSelfPermission(context, permission) ==
                                    PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                viewModel.updateSettings(settings.copy(remindersEnabled = true))
                            } else {
                                notificationPermissionLauncher.launch(permission)
                            }
                        } else {
                            viewModel.updateSettings(settings.copy(remindersEnabled = true))
                        }
                    }
                )
            }

            if (settings.remindersEnabled) {
                // Reminder time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Reminder time:", modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = settings.reminderHour.toString(),
                        onValueChange = { v ->
                            v.toIntOrNull()?.let {
                                viewModel.updateSettings(settings.copy(reminderHour = it.coerceIn(0, 23)))
                            }
                        },
                        label = { Text("Hour") },
                        modifier = Modifier.width(80.dp)
                    )
                    Text(":")
                    OutlinedTextField(
                        value = settings.reminderMinute.toString(),
                        onValueChange = { v ->
                            v.toIntOrNull()?.let {
                                viewModel.updateSettings(settings.copy(reminderMinute = it.coerceIn(0, 59)))
                            }
                        },
                        label = { Text("Min") },
                        modifier = Modifier.width(80.dp)
                    )
                }

                // Day of week chips
                Text("Reminder days", style = MaterialTheme.typography.labelLarge)
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    days.forEachIndexed { index, day ->
                        val bit = 1 shl index
                        FilterChip(
                            selected = (settings.reminderDays and bit) != 0,
                            onClick = {
                                val newDays = settings.reminderDays xor bit
                                viewModel.updateSettings(settings.copy(reminderDays = newDays))
                            },
                            label = { Text(day) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Streak-at-risk alerts")
                Switch(
                    checked = settings.streakRiskNotificationEnabled,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(streakRiskNotificationEnabled = it))
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ---- Display Section ----
            SettingsSectionHeader("Display")

            Text("Theme", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Light", "Dark", "System").forEach { themeOption ->
                    FilterChip(
                        selected = settings.theme == themeOption.uppercase(),
                        onClick = { viewModel.updateSettings(settings.copy(theme = themeOption.uppercase())) },
                        label = { Text(themeOption) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ---- Practice Defaults Section ----
            SettingsSectionHeader("Practice Defaults")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Default practice time", modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        if (settings.defaultSuggestedMinutes > 1) {
                            viewModel.updateSettings(
                                settings.copy(defaultSuggestedMinutes = settings.defaultSuggestedMinutes - 1)
                            )
                        }
                    }
                ) { Text("-", style = MaterialTheme.typography.titleMedium) }
                Text(
                    "${settings.defaultSuggestedMinutes} min",
                    style = MaterialTheme.typography.bodyLarge
                )
                IconButton(
                    onClick = {
                        if (settings.defaultSuggestedMinutes < 60) {
                            viewModel.updateSettings(
                                settings.copy(defaultSuggestedMinutes = settings.defaultSuggestedMinutes + 1)
                            )
                        }
                    }
                ) { Text("+", style = MaterialTheme.typography.titleMedium) }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ---- Danger Zone ----
            SettingsSectionHeader("Danger Zone", color = MaterialTheme.colorScheme.error)

            Button(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete all data")
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete all data?") },
            text = { Text("This will permanently erase your profile, settings, and all practice history. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAllData()
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}
