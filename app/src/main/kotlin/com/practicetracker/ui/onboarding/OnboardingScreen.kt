package com.practicetracker.ui.onboarding

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.io.File

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()

    // Form state – kept local per spec
    var nameField by remember { mutableStateOf("") }
    var instrumentField by remember { mutableStateOf("") }
    var skillLevel by remember { mutableStateOf("") }
    var teacherField by remember { mutableStateOf("") }
    var avatarUriField by remember { mutableStateOf("") }

    // Camera/gallery
    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { avatarUriField = it.toString() }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { avatarUriField = it.toString() }
    }

    fun launchCamera() {
        val photoFile = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    // Back handler
    BackHandler(enabled = currentPage > 0) {
        viewModel.prevPage()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        when (currentPage) {
            0 -> PageNameInstrument(
                name = nameField,
                onNameChange = { nameField = it },
                instrument = instrumentField,
                onInstrumentChange = { instrumentField = it },
                suggestions = viewModel.instrumentSuggestions,
                onNext = { viewModel.nextPage() }
            )
            1 -> PageOptionalDetails(
                avatarUri = avatarUriField,
                skillLevel = skillLevel,
                teacherName = teacherField,
                onSkillLevelChange = { skillLevel = it },
                onTeacherChange = { teacherField = it },
                onLaunchCamera = { launchCamera() },
                onLaunchGallery = { galleryLauncher.launch("image/*") },
                onNext = { viewModel.nextPage() },
                onBack = { viewModel.prevPage() }
            )
            2 -> PageTour(
                onGetStarted = {
                    viewModel.saveProfile(
                        name = nameField,
                        instrument = instrumentField,
                        skillLevel = skillLevel,
                        teacherName = teacherField,
                        avatarUri = avatarUriField
                    )
                    onComplete()
                },
                onBack = { viewModel.prevPage() }
            )
        }

        Spacer(Modifier.height(24.dp))

        // Page indicator
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentPage) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == currentPage)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PageNameInstrument(
    name: String,
    onNameChange: (String) -> Unit,
    instrument: String,
    onInstrumentChange: (String) -> Unit,
    suggestions: List<String>,
    onNext: () -> Unit
) {
    Text(
        "Welcome to PracticeTracker",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Let's set up your profile",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(32.dp))

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Your name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
        value = instrument,
        onValueChange = onInstrumentChange,
        label = { Text("Instrument") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(Modifier.height(8.dp))

    // Instrument suggestion chips
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        suggestions.forEach { suggestion ->
            SuggestionChip(
                onClick = { onInstrumentChange(suggestion) },
                label = { Text(suggestion) }
            )
        }
    }

    Spacer(Modifier.height(32.dp))

    Button(
        onClick = onNext,
        enabled = name.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Next")
    }
}

@Composable
private fun PageOptionalDetails(
    avatarUri: String,
    skillLevel: String,
    teacherName: String,
    onSkillLevelChange: (String) -> Unit,
    onTeacherChange: (String) -> Unit,
    onLaunchCamera: () -> Unit,
    onLaunchGallery: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Text(
        "Tell us more (optional)",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(32.dp))

    // Avatar picker
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { onLaunchGallery() },
        contentAlignment = Alignment.Center
    ) {
        if (avatarUri.isNotBlank()) {
            AsyncImage(
                model = avatarUri,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Filled.CameraAlt,
                contentDescription = "Pick photo",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onLaunchCamera) { Text("Camera") }
        TextButton(onClick = onLaunchGallery) { Text("Gallery") }
    }

    Spacer(Modifier.height(16.dp))

    Text("Skill level", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Beginner", "Intermediate", "Advanced", "Professional").forEach { level ->
            FilterChip(
                selected = skillLevel == level,
                onClick = { onSkillLevelChange(level) },
                label = { Text(level) }
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
        value = teacherName,
        onValueChange = onTeacherChange,
        label = { Text("Teacher name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(Modifier.height(32.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
        Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text("Next") }
    }
}

@Composable
private fun PageTour(
    onGetStarted: () -> Unit,
    onBack: () -> Unit
) {
    Text(
        "Here's how it works",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(24.dp))

    data class FeatureCard(val icon: ImageVector, val title: String, val description: String)

    val features = listOf(
        FeatureCard(
            Icons.Filled.LibraryMusic,
            "Practice",
            "Start a session and work through your practice plan piece by piece"
        ),
        FeatureCard(
            Icons.Filled.BarChart,
            "Stats",
            "View streaks, total practice time, and drill down by piece or skill"
        ),
        FeatureCard(
            Icons.AutoMirrored.Filled.List,
            "Organize",
            "Create practice plans with pieces and skills to focus on"
        )
    )

    features.forEach { feature ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(feature.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        feature.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(32.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
        Button(onClick = onGetStarted, modifier = Modifier.weight(1f)) { Text("Get Started") }
    }
}
