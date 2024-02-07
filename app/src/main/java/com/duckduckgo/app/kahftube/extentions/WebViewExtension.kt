package org.halalz.kahftube.extentions

import android.webkit.WebView

/**
 * Created by Asif Ahmed on 17/1/24.
 */

fun WebView.injectJavascriptFileFromAsset(fileName: String) {
    val jsCode = this.context.readAssetFile(fileName)
    this.evaluateJavascript("javascript:(function() { $jsCode })()", null)
}

fun WebView.clearWebView() {
    this.clearCache(true) // Clear the cache
    this.clearHistory()   // Clear the history
    this.clearFormData()  // Clear the form data

// Additionally, you might want to load a blank page
    this.loadUrl("about:blank") // Load a blank page
}
