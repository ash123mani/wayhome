package com.wayhome.presentation.destination

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wayhome.presentation.designsystem.Space
import com.wayhome.presentation.designsystem.WayHome
import com.wayhome.presentation.designsystem.components.DestinationChip
import com.wayhome.presentation.designsystem.components.SearchAnimation
import com.wayhome.presentation.designsystem.components.Eyebrow
import com.wayhome.presentation.designsystem.components.WayHomeButton
import com.wayhome.presentation.designsystem.components.WayHomeButtonStyle
import com.wayhome.presentation.designsystem.components.WayHomeCard
import kotlinx.coroutines.delay

/**
 * Progressive disclosure: city first, then the area inside it, then a single
 * confirm step. No long forms, no permission asks before the value is set.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DestinationScreen(
    onFound: () -> Unit,
    onBack: (() -> Unit)? = null,
    vm: DestinationViewModel = hiltViewModel()
) {
    val c = WayHome.colors
    val city by vm.selectedCity.collectAsStateWithLifecycle()
    val area by vm.selectedArea.collectAsStateWithLifecycle()
    val customCity by vm.customCity.collectAsStateWithLifecycle()
    val customArea by vm.customArea.collectAsStateWithLifecycle()
    val searching by vm.saving.collectAsStateWithLifecycle()
    val cities = vm.cities

    var cityMenu by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }

    // The scan deserves a beat longer than the API call, so the motion reads.
    LaunchedEffect(scanning) {
        if (scanning) {
            delay(1800)
            vm.saveAndDiscover { onFound() }
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.skyTop, c.background)))) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Space.gutter)
        ) {
            Spacer(Modifier.height(Space.md))
            if (onBack != null) {
                TextButton(onClick = onBack, contentPadding = PaddingZero) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = c.onSurfaceVariant
                    )
                    Spacer(Modifier.width(Space.xs))
                    Text("Back", style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                }
            } else {
                Spacer(Modifier.height(Space.xs))
            }

            Eyebrow("Your route · Two quick steps", color = c.accent)
            Spacer(Modifier.height(Space.md))

            Text(
                "Where are you\nheading home?",
                style = MaterialTheme.typography.displayMedium,
                color = c.onSurface
            )
            Spacer(Modifier.height(Space.lg))
            Text(
                "Pick your city, then the part of it you're going to.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.onSurfaceVariant
            )

            Spacer(Modifier.height(Space.section))

            // Step 1 — city
            StepLabel(step = 1, label = "City")
            Spacer(Modifier.height(Space.sm))
            Box {
                WayHomeCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { cityMenu = true }
                ) {
                    Row(
                        Modifier.padding(horizontal = Space.xl, vertical = Space.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            city.ifBlank { "Choose a city" },
                            style = MaterialTheme.typography.headlineMedium,
                            color = c.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            Icons.Outlined.ExpandMore,
                            contentDescription = "Change city",
                            tint = c.onSurfaceVariant
                        )
                    }
                }
                DropdownMenu(
                    expanded = cityMenu,
                    onDismissRequest = { cityMenu = false }
                ) {
                    cities.forEach { cityOption ->
                        DropdownMenuItem(
                            text = { Text(cityOption.name) },
                            onClick = {
                                vm.selectedCity.value = cityOption.name
                                vm.selectedArea.value = cityOption.areas.firstOrNull()?.name.orEmpty()
                                cityMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(Space.section))

            // Step 2 — area (only once a city is chosen)
            StepLabel(step = 2, label = "Where in $city?")
            Spacer(Modifier.height(Space.md))
            val areas = cities.firstOrNull { it.name == city }?.areas.orEmpty()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                areas.forEach { a ->
                    DestinationChip(
                        label = a.name,
                        selected = a.name == area,
                        onClick = { vm.selectedArea.value = a.name }
                    )
                }
            }

            Spacer(Modifier.height(Space.md))
            Text(
                "Can't find your area? Add it below.",
                style = MaterialTheme.typography.bodySmall,
                color = c.quiet
            )
            Spacer(Modifier.height(Space.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                SmallTextField(
                    value = customCity,
                    onValueChange = { vm.customCity.value = it },
                    placeholder = "Other city",
                    modifier = Modifier.weight(1f)
                )
                SmallTextField(
                    value = customArea,
                    onValueChange = { vm.customArea.value = it },
                    placeholder = "Other area",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(Space.section))

            // Step 3 — confirm
            WayHomeCard(Modifier.fillMaxWidth(), container = c.surfaceMuted) {
                Column(Modifier.padding(Space.xl)) {
                    Text("We'll find people going your way.", style = MaterialTheme.typography.titleLarge, color = c.onSurface)
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        "They'll see only ${resolveLabel(city, area, customCity, customArea)} and your temporary name. Never your exact location.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(Space.xl))
            WayHomeButton(
                text = "Find my way",
                onClick = { scanning = true },
                enabled = !searching,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Space.section))
        }

        // Scan overlay — replaces the form instead of dimming it
        AnimatedVisibility(
            visible = scanning,
            enter = fadeIn() + slideInVertically { it / 6 },
            exit = fadeOut() + slideOutVertically { it / 6 }
        ) {
            ScanOverlay()
        }
    }
}

private val PaddingZero = androidx.compose.foundation.layout.PaddingValues(0.dp, 0.dp, 0.dp, 0.dp)

@Composable
private fun StepLabel(step: Int, label: String) {
    val c = WayHome.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(c.accent),
            contentAlignment = Alignment.Center
        ) {
            Text("$step", style = MaterialTheme.typography.labelMedium, color = c.onAccent)
        }
        Spacer(Modifier.width(Space.sm))
        Text(label, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
    }
}

@Composable
private fun SmallTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val c = WayHome.colors
    Box(modifier.height(48.dp)) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.onSurface),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(c.accent),
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface)
                .border(BorderStroke(1.dp, c.outline), RoundedCornerShape(14.dp))
                .padding(horizontal = Space.lg)
        )
        if (value.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(horizontal = Space.lg),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = c.quiet)
            }
        }
    }
}

private fun resolveLabel(city: String, area: String, customCity: String, customArea: String): String {
    val c = customCity.ifBlank { city }
    val a = customArea.ifBlank { area }
    return if (a.isBlank()) c else "$c · $a"
}

/** Full-bleed scanning state: the phone is visibly looking around you. */
@Composable
private fun ScanOverlay() {
    val c = WayHome.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SearchAnimation(found = 0, diameter = 260.dp)
            Spacer(Modifier.height(Space.section))
            Text(
                "Finding people\ngoing your way…",
                style = MaterialTheme.typography.headlineLarge,
                color = c.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(Space.md))
            Text(
                "Looking around you…",
                style = MaterialTheme.typography.bodyLarge,
                color = c.onSurfaceVariant
            )
        }
    }
}
