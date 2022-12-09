package com.example.vrdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import com.example.vrdemo.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.videorecorder.RootNavigationGraph
import com.example.videorecorder.navigation.Route
import com.example.vrdemo.ui.theme.VRDemoTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionsRequired
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VRDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    RootNavigationGraph()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeUiScreen(navController: NavHostController) {

    val permissionsState = rememberMultiplePermissionsState(permissions = remember {
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    })

    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {

            if (permissionsState.allPermissionsGranted) {
                navController.navigate("cameraScreen")
            } else {
                permissionsState.launchMultiplePermissionRequest()
            }

        }) {
            Text(text = "Offline Recording")
        }
    }
}



@SuppressLint("NewApi")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(navController: NavHostController) {

    val permissionsState = rememberMultiplePermissionsState(permissions = remember {
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    })

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(
        key1 = lifecycleOwner,
        effect = {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) {
                    permissionsState.launchMultiplePermissionRequest()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    )

    val context = LocalContext.current
   // val lifecycleOwner = LocalLifecycleOwner.current

    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    var recording: Recording? = remember { null }
    val previewView: PreviewView = remember { PreviewView(context) }
    val videoCapture: MutableState<VideoCapture<Recorder>?> = remember { mutableStateOf(null) }
    val recordingStarted: MutableState<Boolean> = remember { mutableStateOf(false) }

    val audioEnabled: MutableState<Boolean> = remember { mutableStateOf(true) }
    val cameraSelector: MutableState<CameraSelector> = remember {
        mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA)
    }
    val videoPaused: MutableState<Boolean> = remember { mutableStateOf(false) }



    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }

    LaunchedEffect(previewView) {
        videoCapture.value = context.createVideoCaptureUseCase(
            lifecycleOwner = lifecycleOwner,
            cameraSelector = cameraSelector.value,
            previewView = previewView
        )
    }

    PermissionsRequired(
        multiplePermissionsState = permissionState,
        permissionsNotGrantedContent = { /* ... */ },
        permissionsNotAvailableContent = { /* ... */ }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            Row(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
            , horizontalArrangement = Arrangement.SpaceAround) {

                if (!recordingStarted.value) {
                    IconButton(
                        onClick = {
                            audioEnabled.value = !audioEnabled.value
                        },
                        modifier = Modifier
                            .padding(bottom = 32.dp)
                    ) {
                        Icon(
                            painter = painterResource(if (audioEnabled.value) R.drawable.ic_mic_on else R.drawable.ic_mic_off),
                            contentDescription = "",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                //--->

                if (recordingStarted.value) {
                    IconButton(
                        onClick = {

                            if(!videoPaused.value){
                                recording?.pause()
                                videoPaused.value = true
                            }else{
                                recording?.resume()
                                videoPaused.value = false
                            }
                        },
                        modifier = Modifier
                            .padding(bottom = 32.dp).padding(end = 84.dp)
                    ) {
                        Image(
                            painter = painterResource(if (!videoPaused.value) R.drawable.ic_pause else R.drawable.ic_play),
                            contentDescription = "",
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                //--->

                IconButton(
                    onClick = {
                        if (!recordingStarted.value) {
                            videoCapture.value?.let { videoCapture ->
                                recordingStarted.value = true
                                val mediaDir = context.externalCacheDirs.firstOrNull()?.let {
                                    File(
                                        it,
                                        context.getString(R.string.app_name)
                                    ).apply { mkdirs() }
                                }

                                recording = startRecordingVideo(
                                    context = context,
                                    filenameFormat = "yyyy-MM-dd-HH-mm-ss-SSS",
                                    videoCapture = videoCapture,
                                    outputDirectory = if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir,
                                    executor = context.mainExecutor,
                                    audioEnabled = audioEnabled.value
                                ) { event ->
                                    if (event is VideoRecordEvent.Finalize) {
                                        val uri = event.outputResults.outputUri
                                        if (uri != Uri.EMPTY) {
                                            val uriEncoded = URLEncoder.encode(
                                                uri.toString(),
                                                StandardCharsets.UTF_8.toString()
                                            )
                                            navController.navigate("${Route.VIDEO_PREVIEW}/$uriEncoded")
                                        }
                                    }
                                }
                            }
                        } else {
                            recordingStarted.value = false
                            recording?.stop()
                        }
                    },
                    modifier = Modifier
                        .padding(bottom = 32.dp),
                ) {
                    Image(
                        painter = painterResource(if (recordingStarted.value) R.drawable.ic_stop else R.drawable.ic_record),
                        contentDescription = "",
                        modifier = Modifier.size(84.dp)
                    )
                }
                //--->

                if (!recordingStarted.value) {
                    IconButton(
                        onClick = {
                            cameraSelector.value =
                                if (cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA
                                else CameraSelector.DEFAULT_BACK_CAMERA
                            lifecycleOwner.lifecycleScope.launch {
                                videoCapture.value = context.createVideoCaptureUseCase(
                                    lifecycleOwner = lifecycleOwner,
                                    cameraSelector = cameraSelector.value,
                                    previewView = previewView
                                )
                            }
                        },
                        modifier = Modifier
                            .padding(bottom = 32.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_rotate),
                            contentDescription = "",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    VRDemoTheme {
        HomeUiScreen(navController = rememberNavController())
    }
}