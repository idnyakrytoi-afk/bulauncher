package net.bullmc.client.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.bullmc.client.core.util.SkinFetcher
import org.jetbrains.skia.Image
import java.io.File
import javax.imageio.ImageIO

@Composable
fun SkinPreview(
    nickname: String,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var skinFile by remember { mutableStateOf<File?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(nickname) {
        if (nickname.isBlank()) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        scope.launch(Dispatchers.IO) {
            skinFile = SkinFetcher.getOrPlaceholder(nickname)
            loading = false
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D1117))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF161B22)),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = primaryColor, strokeWidth = 2.dp)
            } else {
                skinFile?.let { file ->
                    val bitmap = remember(file) {
                        try {
                            val img = ImageIO.read(file) ?: return@remember null
                            val baos = java.io.ByteArrayOutputStream()
                            ImageIO.write(img, "png", baos)
                            val skiaImage = Image.makeFromEncoded(baos.toByteArray())
                            skiaImage.toComposeImageBitmap()
                        } catch (_: Exception) { null }
                    }
                    bitmap?.let { bmp ->
                        Image(
                            bitmap = bmp,
                            contentDescription = "Skin",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        if (nickname.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(nickname, fontSize = 11.sp, color = Color(0xFFC9D1D9), fontWeight = FontWeight.Bold)
        }
    }
}
