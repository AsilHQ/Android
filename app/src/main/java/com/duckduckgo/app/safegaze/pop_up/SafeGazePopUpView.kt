import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duckduckgo.app.safegaze.pop_up.SafegazeCloseView
import java.net.URL

@Composable
fun SafegazePopUpView(
    isOpened: Boolean,
    url: URL?,
    updateView: (() -> Unit)?,
    updateBlurIntensity: (() -> Unit)?,
    shieldsSettingsChanged: (() -> Unit)?
) {
    val value = 0

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0.98f, 0.99f, 0.99f),
            Color(1f, 0.98f, 0.99f)
        )
    )

    LaunchedEffect(isOpened) {
        updateView?.invoke()
        shieldsSettingsChanged?.invoke()
    }

    LaunchedEffect(value) {
        if (isOpened) {
            updateBlurIntensity?.invoke()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = gradientBrush,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(vertical = 20.dp, horizontal = 31.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isOpened) {
                SafegazeOpenView(
                )
            } else {
                SafegazeCloseView(
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            ReportView()
        }
    }
}

@Composable
fun ReportView() {
    val reportTitle1 = "Report "
    val reportTitle2 = "this page"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 33.dp)
            .clickable {
                val url = java.net.URL("https://docs.google.com/forms/d/e/1FAIpQLSeaW7PjI-K3yqZZ4gpuXbbx5qOFxAwILLy5uy7PTerXfdzFqw/viewform")
                // Open URL
                // Example: Open URL in browser or webview
            }
    ) {
        // Add your image and text components here
    }
}

@Preview
@Composable
fun SafegazePopUpViewPreview() {
    SafegazePopUpView(
        isOpened = true,
        url = URL("https://example.com"),
        updateView = {},
        updateBlurIntensity = {},
        shieldsSettingsChanged = {}
    )
}
