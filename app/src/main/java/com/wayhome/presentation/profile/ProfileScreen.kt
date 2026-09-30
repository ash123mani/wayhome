package com.wayhome.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wayhome.presentation.designsystem.Space
import com.wayhome.presentation.designsystem.WayHome
import com.wayhome.presentation.designsystem.components.Eyebrow
import com.wayhome.presentation.designsystem.components.RouteTicket
import com.wayhome.presentation.designsystem.components.TravellerAvatar
import com.wayhome.presentation.designsystem.components.WayHomeButton
import com.wayhome.presentation.designsystem.components.WayHomeButtonStyle
import com.wayhome.presentation.designsystem.components.WayHomeCard

@Composable
fun ProfileScreen(
    onChangeDestination: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val c = WayHome.colors
    val profile by vm.profile.collectAsStateWithLifecycle()
    val discoverable = profile?.lookingForPartners == true

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.skyTop, c.background)))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter)
    ) {
        Spacer(Modifier.height(Space.lg))
        Eyebrow("Your pass", color = c.accent)
        Spacer(Modifier.height(Space.sm))
        Text("You", style = MaterialTheme.typography.displayMedium, color = c.onSurface)
        Spacer(Modifier.height(Space.lg))

        // Identity — intentional, not a placeholder
        WayHomeCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.xl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TravellerAvatar(tempId = profile?.tempId.orEmpty(), size = 64.dp)
                    Spacer(Modifier.width(Space.lg))
                    Column {
                        Text(
                            profile?.tempId ?: "Traveller",
                            style = MaterialTheme.typography.headlineMedium,
                            color = c.onSurface
                        )
                        Text(
                            "✈️ Traveling home",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(Space.lg))
                Eyebrow("Temporary identity")
                Spacer(Modifier.height(Space.xs))
                Text(
                    "Yours for this trip only. No name, number or photo is attached to it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(Space.md))

        RouteTicket(
            city = profile?.destination?.city.orEmpty(),
            area = profile?.destination?.area.orEmpty(),
            caption = if (discoverable) "Discoverable" else "Hidden",
            onClick = onChangeDestination
        )

        Spacer(Modifier.height(Space.lg))

        // Discoverability — plain language, one switch
        WayHomeCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.xl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (discoverable) "You're discoverable nearby" else "You're hidden",
                            style = MaterialTheme.typography.titleLarge,
                            color = c.onSurface
                        )
                        Text(
                            if (discoverable) "Travellers around you can see you"
                            else "Nobody can find you right now",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = discoverable,
                        onCheckedChange = { vm.setDiscoverable(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = c.onAccent,
                            checkedTrackColor = c.accent,
                            uncheckedThumbColor = c.quiet,
                            uncheckedTrackColor = c.surfaceMuted,
                            uncheckedBorderColor = c.outlineStrong
                        )
                    )
                }
                Spacer(Modifier.height(Space.lg))
                WayHomeButton(
                    text = "Stop sharing",
                    onClick = { vm.stopSharing() },
                    style = WayHomeButtonStyle.Outline,
                    enabled = discoverable,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(Space.lg))

        // What others can see — reassuring, specific
        WayHomeCard(Modifier.fillMaxWidth(), container = c.surfaceMuted) {
            Column(Modifier.padding(Space.xl)) {
                Text("What others can see", style = MaterialTheme.typography.titleLarge, color = c.onSurface)
                Spacer(Modifier.height(Space.md))
                listOf(
                    "Your temporary name",
                    "Your destination area",
                    "That you're looking for a ride"
                ).forEach { line ->
                    Row(
                        Modifier.padding(vertical = Space.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = c.go,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(Space.sm))
                        Text(line, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
                    }
                }
                Spacer(Modifier.height(Space.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocationOff,
                        contentDescription = null,
                        tint = c.quiet,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(Space.sm))
                    Text(
                        "Never your exact location, address, phone or email",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.xl))
        Text(
            "WayHome never books cabs or handles money. Arrange the ride yourselves — " +
                "meet up, split the fare, done.",
            style = MaterialTheme.typography.bodySmall,
            color = c.quiet
        )
        Spacer(Modifier.height(Space.section))
    }
}
