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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wayhome.presentation.designsystem.CardShape
import com.wayhome.presentation.designsystem.ControlShape
import com.wayhome.presentation.designsystem.Space
import com.wayhome.presentation.designsystem.WayHome
import com.wayhome.presentation.designsystem.aurora
import com.wayhome.presentation.designsystem.WayHomeTravelType
import com.wayhome.presentation.designsystem.auroraWash
import com.wayhome.presentation.designsystem.components.DestinationChip
import com.wayhome.presentation.designsystem.components.Eyebrow
import com.wayhome.presentation.designsystem.components.JourneyRail
import com.wayhome.presentation.designsystem.components.ScreenHeader
import com.wayhome.presentation.designsystem.components.SearchAnimation
import com.wayhome.presentation.designsystem.components.WayHomeButton
import com.wayhome.presentation.designsystem.components.WayHomeButtonStyle
import com.wayhome.presentation.designsystem.components.WayHomeCard
import kotlinx.coroutines.delay

/**
 * Progressive disclosure: city first, then the area inside it, then a single
 * confirm step. No long forms, no permission asks before the value is set, and
 * the trip you're about to announce is previewed on a journey rail.
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

    val areas = cities.firstOrNull { it.name == city }?.areas.orEmpty()
    val resolved = resolveLabel(city, area, customCity, customArea)
    val bothPicked = city.isNotBlank() && area.isNotBlank() ||
        (customCity.isNotBlank() && customArea.isNotBlank())

    Box(Modifier.fillMaxSize().background(c.auroraWash())) {
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
                Row(
                    Modifier
                        .clip(ControlShape)
                        .clickable(onClick = onBack)
                        .padding(horizontal = Space.md, vertical = Space.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = c.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(Space.xs))
                    Text("Back", style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(Space.sm))

            ScreenHeader(
                eyebrow = "Your route",
                title = "Where are you\nheading home?",
                subtitle = "Pick your city, then the part of it you're going to."
            )

            Spacer(Modifier.height(Space.section))

            // Step 1 — city
            StepLabel(step = 1, label = "City", done = city.isNotBlank())
            Spacer(Modifier.height(Space.md))
            Box {
                WayHomeCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { cityMenu = true }
                ) {
                    Row(
                        Modifier.padding(horizontal = Space.xl, vertical = Space.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.LocationCity,
                            contentDescription = null,
                            tint = c.accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(Space.lg))
                        Text(
                            city.ifBlank { "Choose a city" },
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (city.isBlank()) c.quiet else c.onSurface,
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

            // Step 2 — area (only meaningful once a city is chosen)
            StepLabel(
                step = 2,
                label = if (city.isBlank()) "Where in your city?" else "Where in $city?",
                done = area.isNotBlank()
            )
            Spacer(Modifier.height(Space.md))
            if (areas.isEmpty()) {
                Text(
                    "Choose a city above to see its areas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.quiet
                )
            } else {
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
            }

            Spacer(Modifier.height(Space.lg))
            Eyebrow("Or add your own")
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

            // The trip you're about to announce
            WayHomeCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Space.xl)) {
                    Eyebrow("You'll be announced as")
                    Spacer(Modifier.height(Space.lg))
                    JourneyRail(from = city.ifBlank { "—" }, to = area.ifBlank { "—" })
                    Spacer(Modifier.height(Space.lg))
                    Text(
                        if (bothPicked) {
                            "They'll see only $resolved and your temporary name. Never your exact location."
                        } else {
                            "Pick a city and an area to see what other travellers will see."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(Space.xl))
            Spacer(Modifier.height(Space.section))
        }

        // Sticky CTA — always reachable, never scrolled away
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(c.background.copy(alpha = 0f), c.background)
                    )
                )
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Space.gutter, vertical = Space.lg)
        ) {
            WayHomeButton(
                text = "Find my way",
                onClick = { scanning = true },
                enabled = !searching,
                modifier = Modifier.fillMaxWidth()
            )
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

@Composable
private fun StepLabel(step: Int, label: String, done: Boolean) {
    val c = WayHome.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (done) c.go else c.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (done) "✓" else "$step",
                style = WayHomeTravelType.StubLabel,
                color = if (done) c.onAccent else c.onAccentSoft
            )
        }
        Spacer(Modifier.width(Space.md))
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
    Box(modifier.height(52.dp)) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.onSurface),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(c.accent),
            modifier = Modifier
                .fillMaxSize()
                .clip(ControlShape)
                .background(c.surface)
                .border(BorderStroke(1.dp, c.outline), ControlShape)
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
            Box(
                Modifier.size(288.dp).clip(CircleShape).background(c.aurora(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                SearchAnimation(found = 0, diameter = 260.dp)
            }
            Spacer(Modifier.height(Space.section))
            Text(
                "Finding people\ngoing your way…",
                style = MaterialTheme.typography.headlineLarge,
                color = c.onSurface,
                textAlign = TextAlign.Center
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
