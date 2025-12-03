package stream.zappr.tv

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //WebView.setWebContentsDebuggingEnabled(true)

        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)

        setupWebView()
        setupBackHandling()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            setInitialScale(130)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean {
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "onPageFinished: $url")
                    view?.let { injectAndroidTvKeyFilter(it) }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.d(
                        "WebViewConsole",
                        "${consoleMessage.messageLevel()} " +
                            "@${consoleMessage.sourceId()}:${consoleMessage.lineNumber()} " +
                            " -> ${consoleMessage.message()}"
                    )
                    return super.onConsoleMessage(consoleMessage)
                }
            }

            loadUrl(ZAPPR_URL)
        }
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this) {
            if (::webView.isInitialized && webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!::webView.isInitialized || currentFocus !== webView) {
            return super.dispatchKeyEvent(event)
        }

        val keyCode = event.keyCode
        Log.d(TAG, "dispatchKeyEvent: action=${event.action} keyCode=$keyCode")

        if (event.isDpadOrEnter()) {
            val handledByWebView = webView.dispatchKeyEvent(event)
            Log.d(
                TAG,
                "dispatchKeyEvent: ${if (handledByWebView) "handled" else "NOT handled"} by WebView ($keyCode)"
            )
            if (handledByWebView) {
                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    private fun KeyEvent.isDpadOrEnter(): Boolean =
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> true
            else -> false
        }

    private fun injectAndroidTvKeyFilter(webView: WebView) {
        Log.d(TAG, "injectAndroidTvKeyFilter: evaluating JS")
        webView.evaluateJavascript(ANDROID_TV_KEY_FILTER_JS) { value ->
            Log.d(TAG, "injectAndroidTvKeyFilter: JS eval result=$value")
        }
    }

    companion object {
        private const val TAG = "ZapprTV"
        private const val ZAPPR_URL = "https://zappr.stream/?androidtv"
    }
}
