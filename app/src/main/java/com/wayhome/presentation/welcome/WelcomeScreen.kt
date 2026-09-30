package com.wayhome.presentation.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wayhome.presentation.designsystem.Space
import com.wayhome.presentation.designsystem.WayHome
import com.wayhome.presentation.designsystem.WayHomeTravelType
import com.wayhome.presentation.designsystem.components.Eyebrow
import com.wayhome.presentation.designsystem.components.SearchAnimation
import com.wayhome.presentation.designsystem.components.WayHomeButton
import com.wayhome.presentation.designsystem.components.WayHomeButtonStyle
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
            .background(Brush.verticalGradient(listOf(c.skyTop, c.background)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Space.xxl)
    ) {
        Spacer(Modifier.weight(0.5f))

        Eyebrow("WayHome · ride together", color = c.accent)

        Spacer(Modifier.height(Space.lg))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SearchAnimation(found = 3, diameter = 200.dp)
        }

        Spacer(Modifier.height(Space.section))

        Text(
            "You're not the only one heading home.",
            style = MaterialTheme.typography.displayMedium,
            color = c.onSurface
        )
        Spacer(Modifier.height(Space.md))
        Text(
            "WayHome finds the travellers around you going your way — then you can talk, group up and share a cab.",
            style = MaterialTheme.typography.bodyLarge,
            color = c.onSurfaceVariant
        )

        Spacer(Modifier.weight(1f))

        WayHomeButton(
            text = if (profile != null) "Continue as ${profile?.tempId}" else "Get started",
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth()
        )

        if (profile != null) {
            Spacer(Modifier.height(Space.sm))
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
        Text(
            "No signup · No phone number · Works without internet",
            style = WayHomeTravelType.Eyebrow,
            color = c.quiet,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Space.xl))
    }
}
