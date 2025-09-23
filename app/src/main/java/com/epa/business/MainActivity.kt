package com.epa.business

import android.annotation.SuppressLint
//import android.app.Activity
//import android.content.Intent
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
//import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.epa.business.ui.theme.EPATheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EPATheme {
                MainActivityContent()
            }
        }
    }
}

@Composable
fun MainActivityContent() {
    val url = "https://epabusiness.com"
    var showWebView by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    SplashScreen(url = url) {
        showWebView = true // Callback to indicate SplashScreen is complete
        toastMessage = "Splash screen completed!"
    }

    SingleToastContainer(message = toastMessage)
    ClipboardToastManager()

    if (showWebView) {
        WebView(
            url = url,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@SuppressLint("ShowToast")
@Composable
fun ClipboardToastManager() {
    val context = LocalContext.current
    var lastClipboardAction by remember { mutableLongStateOf(0) }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val canShowClipboardToast = remember {
        derivedStateOf {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClipboardAction > 5000 ) {
                lastClipboardAction = currentTime
                true
            } else {
                false
            }
        }
    }

    LaunchedEffect(canShowClipboardToast.value) {
        if(canShowClipboardToast.value) {
            Toast.makeText(context, " ", Toast.LENGTH_SHORT).show()
            lastClipboardAction = System.currentTimeMillis()
        }
    }
}

@Composable
fun SingleToastContainer(message: String?) {
    val showToast = remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            showToast.value = true
            delay(3500) // Adjust the duration as per your requirement
            showToast.value = false
        }
    }
}
@Composable
fun SplashScreen(url: String, onSplashComplete: () -> Unit) {
    var showSplashScreen by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(4000) // Wait for 4 seconds
        showSplashScreen = false // Hide splash screen after 4 seconds
        onSplashComplete() // Callback to notify that splash screen is complete
    }

    if (showSplashScreen) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    VideoView(context).apply {
                        setVideoPath("android.resource://${context.packageName}/raw/epa_splash")
                        start()
                        setOnCompletionListener {
                            showSplashScreen = false
                            onSplashComplete() // Ensure splash is complete after video ends
                        }
                    }
                }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebView(
    url: String,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    val activity = LocalContext.current as ComponentActivity
    val uploadMessageState = remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val result = if (uri != null) arrayOf(uri) else emptyArray<Uri>()
        uploadMessageState.value?.onReceiveValue(result)
        uploadMessageState.value = null
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.setSupportZoom(false) // Disable zooming
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                webViewClient = WebViewClient()
                loadUrl(url)
                onWebViewCreated(this)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        uploadMessageState.value?.onReceiveValue(null)
                        uploadMessageState.value = filePathCallback
                        fileChooserLauncher.launch("image/*")
                        return true
                    }
                }
            }
        }
    )
}
