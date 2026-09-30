package com.wayhome.presentation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wayhome.presentation.chat.ChatScreen
import com.wayhome.presentation.chats.ChatsScreen
import com.wayhome.presentation.destination.DestinationScreen
import com.wayhome.presentation.discovery.DiscoveryScreen
import com.wayhome.presentation.group.GroupDetailScreen
import com.wayhome.presentation.group.GroupListScreen
import com.wayhome.presentation.profile.ProfileScreen
import com.wayhome.presentation.designsystem.WayHome
import com.wayhome.presentation.designsystem.components.WayHomeBottomBar
import com.wayhome.presentation.designsystem.components.WayHomeTab
import com.wayhome.presentation.discovery.DiscoveryViewModel
import com.wayhome.presentation.welcome.WelcomeScreen

object Routes {
    const val WELCOME = "welcome"
    const val DESTINATION = "destination"
    const val HOME = "home"
    const val CHATS = "chats"
    const val GROUPS = "groups"
    const val YOU = "you"
    const val CHAT = "chat?endpointId={endpointId}&tempId={tempId}"
    const val GROUP_DETAIL = "group/{groupId}"

    fun chat(endpointId: String, tempId: String) = "chat?endpointId=$endpointId&tempId=$tempId"
    fun groupDetail(groupId: String) = "group/$groupId"
}

private val tabRoutes = listOf(
    Routes.HOME to WayHomeTab.Home,
    Routes.CHATS to WayHomeTab.Chats,
    Routes.GROUPS to WayHomeTab.Groups,
    Routes.YOU to WayHomeTab.You
)

@Composable
fun WayHomeNav() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val currentTab = tabRoutes.firstOrNull { it.first == currentRoute }?.second

    Box(
        Modifier
            .fillMaxSize()
            .background(WayHome.colors.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                WayHomeNavHost(nav)
            }
            currentTab?.let { tab ->
                WayHomeBottomBar(
                    selected = tab,
                    onSelect = { destination ->
                        if (destination != tab) {
                            nav.navigate(destination.route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}

private val WayHomeTab.route: String
    get() = when (this) {
        WayHomeTab.Home -> Routes.HOME
        WayHomeTab.Chats -> Routes.CHATS
        WayHomeTab.Groups -> Routes.GROUPS
        WayHomeTab.You -> Routes.YOU
    }

@Composable
private fun WayHomeNavHost(nav: NavHostController) {
    NavHost(
        navController = nav,
        startDestination = Routes.WELCOME,
        enterTransition = {
            slideInHorizontally(tween(280)) { it / 6 } + fadeIn(tween(220))
        },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = {
            slideOutHorizontally(tween(240)) { it / 6 } + fadeOut(tween(200))
        }
    ) {
        composable(
            Routes.WELCOME,
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 5 } + fadeOut(tween(200)) }
        ) {
            WelcomeScreen(onGetStarted = { nav.navigate(Routes.DESTINATION) })
        }

        composable(
            Routes.DESTINATION,
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 5 } + fadeOut(tween(200)) }
        ) {
            DestinationScreen(
                onFound = {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.HOME) {
            DiscoveryScreen(
                onOpenChat = { ep, temp -> nav.navigate(Routes.chat(ep, temp)) },
                onOpenGroup = { nav.navigate(Routes.GROUPS) },
                onOpenProfile = { nav.navigate(Routes.YOU) },
                onChangeDestination = { nav.navigate(Routes.DESTINATION) }
            )
        }

        composable(Routes.CHATS) {
            ChatsScreen(
                onOpenDirect = { ep, temp -> nav.navigate(Routes.chat(ep, temp)) },
                onOpenGroup = { gid -> nav.navigate(Routes.groupDetail(gid)) }
            )
        }

        composable(Routes.GROUPS) {
            GroupListScreen(onOpenGroup = { gid -> nav.navigate(Routes.groupDetail(gid)) })
        }

        composable(Routes.YOU) {
            ProfileScreen(onChangeDestination = { nav.navigate(Routes.DESTINATION) })
        }

        composable(
            Routes.CHAT,
            arguments = listOf(
                navArgument("endpointId") { type = NavType.StringType; defaultValue = "" },
                navArgument("tempId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            ChatScreen(
                endpointId = entry.arguments?.getString("endpointId").orEmpty(),
                peerTempId = entry.arguments?.getString("tempId").orEmpty(),
                onBack = { nav.popBackStack() },
                onCreateGroup = { nav.navigate(Routes.GROUPS) }
            )
        }

        composable(
            Routes.GROUP_DETAIL,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { entry ->
            GroupDetailScreen(
                groupId = entry.arguments?.getString("groupId").orEmpty(),
                onBack = { nav.popBackStack() }
            )
        }
    }
}
