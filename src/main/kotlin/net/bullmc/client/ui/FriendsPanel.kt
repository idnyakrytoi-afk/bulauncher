package net.bullmc.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FriendsPanel(primaryColor: Color) {
    val panelShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .padding(start = 8.dp)
            .shadow(4.dp, panelShape)
            .background(Color(0xFF1E1E1E), panelShape)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Friends",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "Онлайн: 3",
                fontSize = 11.sp,
                color = Color(0xFF43A047),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF161616), RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FriendItem(name = "zakuril", status = "В игре", primaryColor = primaryColor)
                    FriendItem(name = "player228", status = "В лобби", primaryColor = primaryColor)
                    FriendItem(name = "DarkLord", status = "Онлайн", primaryColor = primaryColor)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val btnInteraction = remember { MutableInteractionSource() }
            val btnHovered by btnInteraction.collectIsHoveredAsState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (btnHovered) primaryColor.copy(alpha = 0.9f) else primaryColor
                    )
                    .clickable(interactionSource = btnInteraction, indication = null) { },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ Add Friend",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun FriendItem(name: String, status: String, primaryColor: Color) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHovered) Color(0xFF252525) else Color(0xFF1E1E1E))
            .clickable(interactionSource = interactionSource, indication = null) { }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.first().uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(status, fontSize = 10.sp, color = Color(0xFF666666))
        }
    }
}
