package com.wayhome.presentation.welcome

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wayhome.presentation.designsystem.PillShape
import com.wayhome.presentation.designsystem.Space
import com.wayhome.presentation.designsystem.WayHome
import com.wayhome.presentation.designsystem.WayHomeTravelType
import com.wayhome.presentation.designsystem.aurora
import com.wayhome.presentation.designsystem.auroraWash
import com.wayhome.presentation.designsystem.components.SearchAnimation
import com.wayhome.presentation.designsystem.components.WayHomeButton
import com.wayhome.presentation.designsystem.components.Wordmark
import com.wayhome.presentation.profile.ProfileViewModel

/**
 * Brand moment. Answers "who around me is going my way?" in one glance:
 * three travellers converging on one destination.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val c = WayHome.colors
    val profile by vm.profile.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(c.auroraWash())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Space.xxl)
    ) {
        Spacer(Modifier.weight(0.45f))

        Wordmark()

        Spacer(Modifier.weight(0.3f))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(238.dp)
                    .clip(CircleShape)
                    .background(c.aurora(alpha = 0.10f))
            )
            SearchAnimation(found = 3, diameter = 210.dp, decorative = true)
        }

        Spacer(Modifier.weight(0.45f))

        Text(
            "You're not the only one heading home.",
            style = WayHomeTravelType.Hero,
            color = c.onSurface
        )
        Spacer(Modifier.height(Space.md))
        Text(
            "WayHome finds the travellers around you going your way — then you can talk, group up and share a cab.",
            style = MaterialTheme.typography.bodyLarge,
            color = c.onSurfaceVariant
        )

        Spacer(Modifier.weight(1f))

        // Trust strip — the three promises, as departure-board data
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            listOf("No signup", "No numbers", "Works offline").forEach { promise ->
                Text(
                    promise.uppercase(),
                    style = WayHomeTravelType.StubLabel,
                    color = c.onSurfaceVariant,
                    modifier = Modifier
                        .clip(PillShape)
                        .background(c.surface)
                        .padding(horizontal = Space.md, vertical = 7.dp)
                )
            }
        }

        Spacer(Modifier.height(Space.lg))

        WayHomeButton(
            text = if (profile != null) "Continue as ${profile?.tempId}" else "Get started",
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth()
        )

        if (profile != null) {
            Spacer(Modifier.height(Space.xs))
            Text(
                "Start over with a new temporary name",
                style = MaterialTheme.typography.bodySmall,
                color = c.quiet,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onGetStarted)
                    .padding(vertical = Space.md),
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(Space.lg))
    }
}
