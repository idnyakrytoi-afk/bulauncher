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
import net.bullmc.client.theme.LocalBullColors
import org.jetbrains.skia.Image
import java.io.File
import javax.imageio.ImageIO

@Composable
fun SkinPreview(
    nickname: String,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalBullColors.current
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
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(10.dp)).background(colors.surfaceSunken),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.primary, strokeWidth = 2.dp)
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
            Text(nickname, fontSize = 11.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold)
        }
    }
}
