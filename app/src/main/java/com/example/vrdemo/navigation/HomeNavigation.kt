package com.example.videorecorder.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.videorecorder.navigation.Route.VIDEO_PREVIEW_ARG
import com.example.videorecorder.navigation.Route.VIDEO_PREVIEW_FULL_ROUTE
import com.example.vrdemo.CameraScreen
import com.example.vrdemo.HomeUiScreen
import com.example.vrdemo.RecordingScreenViewModel
import com.example.vrdemo.VideoPreviewScreen


@RequiresApi(Build.VERSION_CODES.P)
fun NavGraphBuilder.homeNavGraph(
    navController: NavHostController,
    recordingScreenViewModel: RecordingScreenViewModel
) {
    navigation(
        route = "home_graph",
        startDestination = "homeView"
    ) {
        composable(route = "homeView") {
            HomeUiScreen(navController)
        }
        composable(route = "cameraScreen") {
            CameraScreen(navController)
        }
        composable(route = VIDEO_PREVIEW_FULL_ROUTE) {
            val uri = it.arguments?.getString(VIDEO_PREVIEW_ARG) ?: ""
            VideoPreviewScreen(uri = uri)
        }
    }
}

object Route {
    const val VIDEO = "video"
    const val VIDEO_PREVIEW_FULL_ROUTE = "video_preview/{uri}"
    const val VIDEO_PREVIEW = "video_preview"
    const val VIDEO_PREVIEW_ARG = "uri"
}