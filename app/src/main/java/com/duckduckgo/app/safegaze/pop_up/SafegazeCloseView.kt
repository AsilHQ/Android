package com.duckduckgo.app.safegaze.pop_up

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

@Composable
fun SafegazeCloseView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContextCompat.getDrawable(LocalContext.current, android.R.drawable.alert_dark_frame)?.toBitmap()?.asImageBitmap()
            ?.let { imageBitmap ->
                HeaderView(image = imageBitmap)
            }
        Spacer(modifier = Modifier.height(16.dp))
        SafegazeHostView(url = "Hello world")
        Spacer(modifier = Modifier.height(16.dp))
        ToggleView()
        Spacer(modifier = Modifier.height(16.dp))
        DescriptionView()
    }
}

@Composable
private fun HeaderView(image: ImageBitmap) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        ResizableImageView(width = 44, height = 40, imageName = "safe_gaze_icon")
        Spacer(modifier = Modifier)
    }
}

@Composable
private fun ToggleView(){
    Button(
        onClick = {
            println("Hello World")
        },
    ) {
        ResizableImageView(width = 136, height = 136)
    }
}

@Composable
fun DescriptionView() {
    Box(
        modifier = Modifier
            .padding(horizontal = 17.dp)
            .height(73.dp)
            .background(Color(0.93f, 0.93f, 0.94f), shape = RoundedCornerShape(10.dp))
            .padding(3.dp)

    ) {
        Text(
            text = "Take control of your digital space with our Ad Blocker and Image Purificator extension combo. Savegaze will save your eyes from singul act.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            textAlign = TextAlign.Center,
            color = Color(0.27f, 0.27f, 0.27f)
        )
    }
}
@Preview
@Composable
fun SafegazeCloseViewPreview() {
    SafegazeCloseView()
}
