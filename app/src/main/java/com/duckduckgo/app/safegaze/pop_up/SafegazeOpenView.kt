package com.duckduckgo.app.safegaze.pop_up

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duckduckgo.app.browser.R

@Composable
fun SafegazeOpenView(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 18.dp)) {
        OpenHeaderView()
        Spacer(modifier = Modifier.height(16.dp))
        OpenHostView("Hello World")
        Spacer(modifier = Modifier.height(16.dp))
        OpenContentStack(domainAvoidedContentCount = 0, lifetimeAvoidedContentCount = 0)
        Spacer(modifier = Modifier.height(16.dp))
        GenderModeView()
    }
}

@Composable
fun OpenHeaderView() {
    var isOn by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(end = 17.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ResizableImageView(imageName = "safe_gaze_icon", width = 44, height = 40)

            Column(
                modifier = Modifier
                    .width(148.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(end = 14.dp)
                    .border(1.dp, Color(0.91f, 0.91f, 0.91f), RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Safegaze Up",
                        modifier = Modifier.width(76.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = isOn,
                        onCheckedChange = { isOn = it },
                        modifier = Modifier
                            .width(28.dp)
                            .height(16.dp)
                            .scale(0.6f)
                            .padding(end = 14.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0.06f, 0.7f, 0.79f),
                            uncheckedThumbColor = Color.Gray
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun OpenHostView(url: String) {
    var selection by remember { mutableIntStateOf(0) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.White)
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(10.dp))
            .shadow(elevation = 2.5.dp, shape = RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SafegazeHostView(url = url)
        Spacer(modifier = Modifier.weight(1f))
    }
}



@Composable
fun OpenContentStack(domainAvoidedContentCount: Int, lifetimeAvoidedContentCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(10.dp))
            .shadow(elevation = 2.5.dp, shape = RoundedCornerShape(10.dp))
    ) {
        Text(
            text = "Sinful acts avoided",
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(end = 14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp)) // Add space between title and content
        Row {

            Row {
                SafegazeCircleCountView(count = domainAvoidedContentCount)
                Text(
                    text = "This Page",
                    fontWeight = FontWeight.Medium,
                    color = Color(0.43f, 0.43f, 0.43f)
                )
            }

            Row {
                SafegazeCircleCountView(count = lifetimeAvoidedContentCount)

                Text(
                    text = "Life time",
                    fontWeight = FontWeight.Medium,
                    color = Color(0.43f, 0.43f, 0.43f)
                )
            }

        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun GenderModeView() {
    Column(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .background(color = Color.White, shape = RoundedCornerShape(10.dp))
            .shadow(elevation = 2.5.dp, shape = RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 13.dp)
                .padding(bottom = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Gender Mode",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                ResizableImageView(
                    image = painterResource(id = R.drawable.add_widget_cta_icon),
                    width = 13,
                    height = 13,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .padding(5.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(red = 0.62f, green = 0.48f, blue = 0.92f, alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
            ) {
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0.62f, 0.48f, 0.92f),
                    modifier = Modifier.graphicsLayer(rotationZ = -10f)
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier
                .padding(horizontal = 13.dp)
                .height(45.dp)
                .background(
                    color = Color(0.97f, 0.96f, 0.96f),
                    shape = RoundedCornerShape(61.dp)
                )
        ) {
            GenderButton(icon = R.drawable.add_widget_cta_icon, text = "Man")
            Spacer(modifier = Modifier.width(5.dp))
            GenderButton(icon = R.drawable.add_widget_cta_icon, text = "Woman")
        }
    }
}

@Composable
fun GenderButton(icon: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxHeight()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.White,
                        Color.Transparent,
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 8.dp)
    ) {
        ResizableImageView(
            image = painterResource(id = icon),
            width = 20,
            height = 20,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = Color(0.06f, 0.7f, 0.79f)
        )
    }
}

@Preview
@Composable
fun SafegazeOpenViewPreview() {
    SafegazeOpenView()
}

