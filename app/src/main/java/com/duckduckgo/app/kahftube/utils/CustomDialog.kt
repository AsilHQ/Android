package com.duckduckgo.app.kahftube.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.browser.databinding.DialogChannelListBinding
import com.duckduckgo.app.browser.databinding.DialogProgressBinding
import com.duckduckgo.app.kahftube.model.ChannelModel
import com.duckduckgo.app.kahftube.view.adapter.ChannelListAdapter

class CustomDialog(val context: Context) {

    var dialog: Dialog = Dialog(context, R.style.Theme_KahfTube)

    fun progressDialog(): Dialog {
        val binding = DialogProgressBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.setCancelable(true)
        return dialog
    }

    fun showChannelListDialog(
        title: String,
        channelList: List<ChannelModel>,
        cancelClickListener: () -> Unit,
        unsubscribeClickListener: () -> Unit
    ): Dialog {
        val binding = DialogChannelListBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set title
        binding.textTitle.text = title

        // Set up RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = ChannelListAdapter(channelList)

        binding.buttonCancel.setOnClickListener {
            cancelClickListener.invoke()
            dialog.dismiss()
        }

        binding.buttonUnsubscribe.setOnClickListener {
            unsubscribeClickListener.invoke()
            dialog.dismiss()
        }
        dialog.show()
        return dialog
    }
}

interface ConfirmDialogInterface {
    fun yes()
    fun no()
}
