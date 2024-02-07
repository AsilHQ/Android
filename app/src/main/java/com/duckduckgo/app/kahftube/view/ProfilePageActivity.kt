package org.halalz.kahftube.view

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.duckduckgo.app.browser.R.string
import com.duckduckgo.app.browser.databinding.ActivityProfilePageBinding
import com.duckduckgo.app.kahftube.KahfTubeInterface
import com.duckduckgo.app.kahftube.KahfTubeInterface.JavaScriptCallBack
import com.duckduckgo.app.kahftube.KahfTubeUnsubscribeInterface
import com.duckduckgo.app.kahftube.SharedPreferenceManager
import com.duckduckgo.app.kahftube.SharedPreferenceManager.KeyString
import com.duckduckgo.app.kahftube.model.ChannelModel
import com.duckduckgo.app.kahftube.utils.CustomDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.halalz.kahftube.extentions.injectJavascriptFileFromAsset
import org.halalz.kahftube.extentions.loadFromUrl
import org.json.JSONArray
import timber.log.Timber

class ProfilePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilePageBinding
    private lateinit var sharedPref: SharedPreferenceManager
    private lateinit var progressDialog: Dialog
    private lateinit var subscribedChannelDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfilePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = SharedPreferenceManager(this@ProfilePageActivity)
        //saveTempDummyInfo()
        prepareViews()
        initListeners()
    }

    private fun saveTempDummyInfo() {
        sharedPref.setValue(KeyString.NAME, "Guest")
        sharedPref.setValue(KeyString.EMAIL, "guest@gmail.com")
        sharedPref.setValue(KeyString.IMAGE_SRC, "https://yt3.ggpht.com/yti/AGOGRCqFABQK7A5GvVn7iUHY3AWK8GX182NEWZAXhw=s88-c-k-c0x00ffffff-no-rj")
        sharedPref.setValue(KeyString.PRACTICING_LEVEL, 1)
        sharedPref.setValue(KeyString.GENDER, 2)
        sharedPref.setValue(KeyString.TOKEN, "1637|VTwoGKekKw5uLze11HEwQc1kExtFnkuqts0chlOw1c46b4ff")
    }

    private fun initListeners() {
        binding.layoutPreference.setOnClickListener {
            startActivity(Intent(this@ProfilePageActivity, PreferencePageActivity::class.java))
        }
        binding.layoutUnsubscribe.setOnClickListener {
            getSubscribedChannel()
            // showSubscribedChannelDialog()
        }
    }

    private fun showSubscribedChannelDialog(channelList: List<ChannelModel>) {
        val dialogTitle = getString(string.haram_channels_found, channelList.size)
        subscribedChannelDialog = CustomDialog(this).showChannelListDialog(
            title = dialogTitle,
            channelList = channelList,
            cancelClickListener = {

            },
            unsubscribeClickListener = {
                unsubscribe(channelList.map { it.id })
            },
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun unsubscribe(channelList: List<String>) {
        Timber.d("KahfTubeUnsubscribeInterface:: Unsubscribe:: channelList: $channelList")
        //showEmailAccessForKahfTubeDialog()
        progressDialog.show()
        binding.headlessKahfTubeWebview.apply {
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(
                    webView: WebView?,
                    url: String?
                ) {
                    super.onPageFinished(webView, url)
                    if (binding.headlessKahfTubeWebview.progress >= 100) {
                        Timber.v("onPageFinished.url: $url")
                        webView?.injectJavascriptFileFromAsset("kahftube/unsubscribe.js")
                    }
                }
            }
            addJavascriptInterface(
                KahfTubeUnsubscribeInterface(
                    channelList,
                    object : KahfTubeUnsubscribeInterface.JavaScriptCallBack {
                        override fun responseCallback(isSuccess: Boolean) {
                            lifecycleScope.launch {
                                progressDialog.dismiss()
                                if (isSuccess) {
                                    Snackbar.make(binding.root, "Unsubscribed successfully.", Snackbar.LENGTH_LONG).show()
                                } else {
                                    Snackbar.make(binding.root, "Please try again.", Snackbar.LENGTH_LONG).show()
                                }

                            }
                        }
                    },
                ),
                "KahfTubeUnsubscribeInterface",
            )
            loadUrl("https://www.youtube.com")
        }
    }

    private fun prepareViews() {
        binding.toolbar.setNavigationIcon(com.duckduckgo.mobile.android.R.drawable.ic_arrow_left_24)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        progressDialog = CustomDialog(this).progressDialog()
        binding.textName.text = sharedPref.getValue(KeyString.NAME)
        binding.imageProfile.loadFromUrl(sharedPref.getValue(KeyString.IMAGE_SRC))
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun getSubscribedChannel() {
        //showEmailAccessForKahfTubeDialog()
        progressDialog.show()
        binding.headlessKahfTubeWebview.apply {
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(
                    webView: WebView?,
                    url: String?
                ) {
                    super.onPageFinished(webView, url)
                    if (binding.headlessKahfTubeWebview.progress >= 100) {
                        Timber.v("onPageFinished.url: $url")
                        webView?.injectJavascriptFileFromAsset("kahftube/channel.js")
                    }
                }
            }
            addJavascriptInterface(
                KahfTubeInterface(
                    this@ProfilePageActivity,
                    object :
                        JavaScriptCallBack {
                        override fun getChannels(jsonArrayString: String) {
                            Timber.d("Channel List: ${jsonArrayString}")
                            progressDialog.dismiss()

                            val jsonArray = JSONArray(jsonArrayString)

                            val channelList = mutableListOf<ChannelModel>()

                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val channel = ChannelModel(
                                    id = jsonObject.getString("id"),
                                    name = jsonObject.getString("name"),
                                    isUnsubscribed = jsonObject.getBoolean("isUnsubscribed"),
                                    thumbnail = jsonObject.getString("thumbnail"),
                                    isHaram = jsonObject.getBoolean("isHaram"),
                                )
                                channelList.add(channel)
                            }
                            lifecycleScope.launch {
                                //binding.headlessKahfTubeWebview.clearWebView()
                                if (channelList.isNotEmpty()) {
                                    showSubscribedChannelDialog(channelList)
                                } else {
                                    Snackbar.make(binding.root, "No haram channel found", Snackbar.LENGTH_LONG).show()
                                }

                            }
                        }

                        override fun shouldRestart() {
                            //recreate()
                        }
                    },
                ),
                "KahfTubeInterface",
            )
            loadUrl("https://m.youtube.com/feed/channels")
        }
    }
}
