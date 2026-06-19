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
            .width(240.dp)
            .fillMaxHeight()
            .padding(start = 8.dp)
            .shadow(4.dp, panelShape)
            .background(Color(0xFF161B22), panelShape)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\u0414\u0440\u0443\u0437\u044C\u044F",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC9D1D9)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(primaryColor.copy(alpha = 0.15f))
                        .clickable {},
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 12.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "\u25CF  3 \u0432 \u0441\u0435\u0442\u0438",
                fontSize = 11.sp,
                color = Color(0xFF34D399),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117), RoundedCornerShape(10.dp))
                    .padding(6.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FriendItem(name = "zakuril", status = "\u0412 \u0438\u0433\u0440\u0435", primaryColor = primaryColor)
                    FriendItem(name = "player228", status = "\u0412 \u043B\u043E\u0431\u0431\u0438", primaryColor = primaryColor)
                    FriendItem(name = "DarkLord", status = "\u041E\u043D\u043B\u0430\u0439\u043D", primaryColor = primaryColor)
                }
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
            .background(if (isHovered) Color(0xFF1C2028) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null) { }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(primaryColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.first().uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFC9D1D9))
            Text(status, fontSize = 9.sp, color = Color(0xFF484F58))
        }
    }
}
