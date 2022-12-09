package com.example.videorecorder

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.videorecorder.navigation.homeNavGraph
import com.example.vrdemo.RecordingScreenViewModel

@Composable
fun RootNavigationGraph() {

    val navController: NavHostController = rememberNavController()

    val recordingScreenViewModel = RecordingScreenViewModel()


    NavHost(
        navController = navController,
        route = "root_graph",
        startDestination = "home_graph"
    ) {
        homeNavGraph(navController,recordingScreenViewModel)
    }
}