package com.automatelinux.trips.ui.feedback

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.automatelinux.feedbacklib.ui.issues.FeedbackIssuesScreen
import com.automatelinux.trips.BuildConfig
import com.automatelinux.trips.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedbackIssuesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                FeedbackIssuesScreen(onNavigateBack = { finish() }, versionName = BuildConfig.VERSION_NAME)
            }
        }
    }
}
