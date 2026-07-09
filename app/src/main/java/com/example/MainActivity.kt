package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BlemixoViewModel
import com.example.ui.screens.*
import com.example.ui.theme.BlemixoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BlemixoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentUser by viewModel.currentUser.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val notificationMsg by viewModel.notification.collectAsState()

            val isDarkMode = currentUser?.isDarkMode ?: false

            BlemixoTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Core Application Navigation Selector
                        val isLoggedIn = currentUser?.isLoggedIn == true

                        if (!isLoggedIn) {
                            LoginScreen(viewModel)
                        } else {
                            val configuration = LocalConfiguration.current
                            val isTablet = configuration.screenWidthDp >= 600

                            when (currentScreen) {
                                "login" -> {
                                    LoginScreen(viewModel)
                                }
                                "home" -> {
                                    MainScreen(viewModel)
                                }
                                "chat" -> {
                                    if (isTablet) {
                                        // On tablets, chat is rendered in Master-Detail inside MainScreen
                                        MainScreen(viewModel)
                                    } else {
                                        // On mobile, chat is a dedicated full-screen page
                                        ChatScreen(viewModel)
                                    }
                                }
                                "call" -> {
                                    VoiceCallScreen(viewModel)
                                }
                                "videocall" -> {
                                    VideoCallScreen(viewModel)
                                }
                                else -> {
                                    MainScreen(viewModel)
                                }
                            }
                        }

                        // Premium Dynamic Custom Notification Toast
                        AnimatedVisibility(
                            visible = notificationMsg != null,
                            enter = slideInVertically(
                                initialOffsetY = { -it },
                                animationSpec = tween(400)
                            ) + fadeIn(),
                            exit = slideOutVertically(
                                targetOffsetY = { -it },
                                animationSpec = tween(300)
                            ) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        ) {
                            notificationMsg?.let { msg ->
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.dismissNotification() }
                                        .testTag("notification_banner")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.NotificationsActive,
                                                contentDescription = "Notification",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = msg,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.dismissNotification() },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Close notification",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
