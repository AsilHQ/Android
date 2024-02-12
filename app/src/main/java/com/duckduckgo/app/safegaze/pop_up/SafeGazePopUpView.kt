import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duckduckgo.app.safegaze.pop_up.ResizableImageView
import com.duckduckgo.app.safegaze.pop_up.SafegazeCloseView

@Composable
fun SafegazePopUpView(
    isOpened: Boolean
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0.98f, 0.99f, 0.99f),
            Color(1f, 0.98f, 0.99f)
        )
    )
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
    val url = "https://docs.google.com/forms/d/e/1FAIpQLSeaW7PjI-K3yqZZ4gpuXbbx5qOFxAwILLy5uy7PTerXfdzFqw/viewform"
    val context = LocalContext.current

    TextButton(
        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
        modifier = Modifier.padding(horizontal = 33.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ResizableImageView(imageName = "", width = 16, height = 16)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Please report any bugs or suggestions to this form", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Strings.safegazePopupReportTitle2",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0.06f, 0.7f, 0.79f, 1f))
            )
        }
    }
}

@Preview
@Composable
fun SafegazePopUpViewPreview() {
    SafegazePopUpView(
        isOpened = false
    )
}
