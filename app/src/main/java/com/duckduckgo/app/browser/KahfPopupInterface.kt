import android.content.SharedPreferences.Editor
import android.webkit.JavascriptInterface
import com.duckduckgo.app.browser.DuckDuckGoWebView
import com.duckduckgo.common.utils.DispatcherProvider
import com.duckduckgo.common.utils.SAFE_GAZE_ACTIVE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PopupWebViewInterface(
    private val webView: DuckDuckGoWebView?,
    private val dispatcherProvider: DispatcherProvider,
    private val editor: Editor
) {

    @JavascriptInterface
    fun toggleSageGaze(state: Boolean) {
        editor.putBoolean(SAFE_GAZE_ACTIVE, state)
        editor.apply()

        CoroutineScope(dispatcherProvider.main()).launch {
            webView?.reload()
        }
    }
}
