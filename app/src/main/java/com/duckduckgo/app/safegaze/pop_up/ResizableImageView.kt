package com.duckduckgo.app.safegaze.pop_up

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duckduckgo.app.browser.R

@Composable
fun ResizableImageView(
    imageName: String? = null,
    image: Painter? = null,
    width: Int,
    height: Int
) {
    Box(
        modifier = Modifier
            .size(width.dp, height.dp)
            .wrapContentSize(Alignment.Center)
    ) {
        if (imageName != null) {
            Image(
                painter = painterResource(id = getResourceId(imageName)),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else if (image != null) {
            Image(
                painter = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun getResourceId(imageName: String): Int {
    return try {
        R.drawable::class.java.getField(imageName).getInt(null)
    } catch (e: Exception) {
        R.drawable.add_widget_cta_icon
    }
}
@Preview
@Composable
fun ResizableImageViewPreview(){
    ResizableImageView(width = 44, height = 40, imageName = "safe_gaze_icon")
}


