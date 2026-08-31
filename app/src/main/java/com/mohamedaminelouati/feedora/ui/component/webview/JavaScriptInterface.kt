package com.mohamedaminelouati.feedora.ui.component.webview

import android.webkit.JavascriptInterface

interface JavaScriptInterface {

    @JavascriptInterface
    fun onImgTagClick(imgUrl: String?, alt: String?)

    companion object {

        const val NAME = "JavaScriptInterface"
    }
}
