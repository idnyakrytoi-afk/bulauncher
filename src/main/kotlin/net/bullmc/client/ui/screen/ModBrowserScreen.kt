package net.bullmc.client.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Text
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.bullmc.client.core.mod.BrowserMod
import net.bullmc.client.core.mod.CurseForgeApi
import net.bullmc.client.core.mod.ModSource
import net.bullmc.client.core.mod.ModrinthApi
import net.bullmc.client.core.loader.LoaderType
import java.io.File

@Composable
fun ModBrowserScreen(
    primaryColor: Color,
    mcVersion: String,
    loader: LoaderType,
    installedModSlugs: List<String>,
    modsDir: File,
    onModInstalled: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val modrinthApi = remember { ModrinthApi() }
    val curseForgeApi = remember { CurseForgeApi() }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<BrowserMod>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedSource by remember { mutableStateOf(ModSource.MODRINTH) }
    var selectedMod by remember { mutableStateOf<BrowserMod?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    var showSourceMenu by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedSort by remember { mutableStateOf("relevance") }

    val categories = listOf(
        "all" to "\u2605 Все",
        "optimization" to "\u26A1 Оптимизация",
        "rendering" to "\uD83C\uDFA8 Рендеринг",
        "utility" to "\uD83D\uDD27 Утилиты",
        "technology" to "\u2699\uFE0F Технологии",
        "adventure" to "\u2694\uFE0F Приключения",
        "magic" to "\u2728 Магия",
        "storage" to "\uD83D\uDCE6 Хранилище",
        "farming" to "\uD83C\uDF3E Фермерство",
        "decoration" to "\uD83C\uDFE0 Декор",
        "mobs" to "\uD83D\uDC3E Мобы",
        "food" to "\uD83C\uDF5C Еда",
        "library" to "\uD83D\uDCDA Библиотеки",
        "worldgen" to "\uD83C\uDF0D Генерация мира",
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text("Магазин модов", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE6EDF3))
        Text(
            "Скачивайте моды с Modrinth и CurseForge",
            fontSize = 14.sp, color = Color(0xFF6E7681),
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1C2128))
                        .clickable { showSourceMenu = true }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            selectedSource.displayName,
                            fontSize = 14.sp,
                            color = if (selectedSource == ModSource.MODRINTH) Color(0xFF34D399) else Color(0xFFF97316)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("\u25BC", fontSize = 10.sp, color = Color(0xFF8B949E))
                    }
                }
                DropdownMenu(
                    expanded = showSourceMenu,
                    onDismissRequest = { showSourceMenu = false },
                    modifier = Modifier.background(Color(0xFF161B22))
                ) {
                    DropdownMenuItem(onClick = {
                        selectedSource = ModSource.MODRINTH
                        showSourceMenu = false
                        searchResults = emptyList()
                    }) {
                        Text("Modrinth", color = Color(0xFF34D399))
                    }
                    DropdownMenuItem(onClick = {
                        selectedSource = ModSource.CURSEFORGE
                        showSourceMenu = false
                        searchResults = emptyList()
                    }) {
                        Text("CurseForge", color = Color(0xFFF97316))
                    }
                }
            }

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск модов...", color = Color(0xFF8B949E), fontSize = 14.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFE6EDF3), fontSize = 14.sp),
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color(0xFF1C2128),
                    cursorColor = primaryColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(primaryColor)
                    .clickable {
                        if (searchQuery.isNotBlank()) {
                            scope.launch(Dispatchers.IO) {
                                isLoading = true
                                statusMessage = "Поиск в ${selectedSource.displayName}..."
                                val results = when (selectedSource) {
                                    ModSource.MODRINTH -> modrinthApi.searchMods(
                                        searchQuery, mcVersion, loader, category = selectedCategory, sort = selectedSort
                                    )
                                    ModSource.CURSEFORGE -> curseForgeApi.searchMods(
                                        searchQuery, mcVersion, loader, category = selectedCategory, sort = selectedSort
                                    )
                                }
                                withContext(Dispatchers.Main) {
                                    searchResults = results.map { mod ->
                                        mod.copy(installed = mod.slug in installedModSlugs)
                                    }
                                    isLoading = false
                                    statusMessage = if (results.isEmpty()) "Ничего не найдено" else ""
                                }
                            }
                        }
                    }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("\uD83D\uDD0D Поиск", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (statusMessage.isNotEmpty() && !isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(statusMessage, fontSize = 13.sp, color = Color(0xFF8B949E))
        }

        Spacer(modifier = Modifier.height(10.dp))

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(categories.size) { index ->
                val (catId, catLabel) = categories[index]
                val isSelected = if (catId == "all") selectedCategory == null else selectedCategory == catId
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) primaryColor else Color(0xFF1C2128))
                        .clickable {
                            selectedCategory = if (catId == "all") null else catId
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        catLabel,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xFF8B949E)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Сортировка:", fontSize = 12.sp, color = Color(0xFF6E7681))
            val sortOptions = listOf(
                "relevance" to "По релевантности",
                "downloads" to "По популярности",
                "newest" to "По дате"
            )
            for ((sortId, sortLabel) in sortOptions) {
                val isSelected = selectedSort == sortId
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) primaryColor.copy(alpha = 0.2f) else Color(0xFF0D1117))
                        .clickable {
                            selectedSort = sortId
                        }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        sortLabel,
                        fontSize = 11.sp,
                        color = if (isSelected) primaryColor else Color(0xFF8B949E)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedMod != null) {
            ModDetailPanel(
                mod = selectedMod!!,
                primaryColor = primaryColor,
                mcVersion = mcVersion,
                loader = loader,
                modsDir = modsDir,
                onBack = { selectedMod = null },
                onInstalled = { slug ->
                    selectedMod = selectedMod?.copy(installed = true)
                    searchResults = searchResults.map { if (it.slug == slug) it.copy(installed = true) else it }
                    onModInstalled(slug)
                }
            )
        } else if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = primaryColor, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(statusMessage, fontSize = 14.sp, color = Color(0xFF8B949E))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(searchResults) { mod ->
                    ModListItem(
                        mod = mod,
                        primaryColor = primaryColor,
                        isInstalled = mod.slug in installedModSlugs,
                        onClick = { selectedMod = mod }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModListItem(
    mod: BrowserMod,
    primaryColor: Color,
    isInstalled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isInstalled) primaryColor.copy(alpha = 0.08f) else Color(0xFF161B22))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (mod.iconUrl != null) {
            ModIcon(mod.iconUrl, mod.name)
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    mod.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE6EDF3),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isInstalled) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("\u2714", fontSize = 12.sp, color = Color(0xFF34D399))
                }
            }
            Text(
                mod.description,
                fontSize = 12.sp,
                color = Color(0xFF8B949E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (mod.author.isNotEmpty()) {
                    Text(mod.author, fontSize = 11.sp, color = Color(0xFF6E7681))
                }
                Text("${formatDownloads(mod.downloads)} загрузок", fontSize = 11.sp, color = Color(0xFF6E7681))
                Text(
                    if (mod.source == ModSource.MODRINTH) "Modrinth" else "CurseForge",
                    fontSize = 11.sp,
                    color = if (mod.source == ModSource.MODRINTH) Color(0xFF34D399) else Color(0xFFF97316)
                )
            }
        }
    }
}

@Composable
private fun ModDetailPanel(
    mod: BrowserMod,
    primaryColor: Color,
    mcVersion: String,
    loader: LoaderType,
    modsDir: File,
    onBack: () -> Unit,
    onInstalled: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var downloadStatus by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(primaryColor.copy(alpha = 0.1f))
                .clickable { onBack() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("\u2190", fontSize = 16.sp, color = primaryColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Назад к списку", fontSize = 14.sp, color = primaryColor)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF161B22))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (mod.iconUrl != null) {
                ModIconLarge(mod.iconUrl, mod.name)
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(mod.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE6EDF3))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (mod.author.isNotEmpty()) {
                        Text("by ${mod.author}", fontSize = 13.sp, color = Color(0xFF8B949E))
                    }
                    Text("${formatDownloads(mod.downloads)} загрузок", fontSize = 13.sp, color = Color(0xFF8B949E))
                    Text(
                        if (mod.source == ModSource.MODRINTH) "Modrinth" else "CurseForge",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (mod.source == ModSource.MODRINTH) Color(0xFF34D399) else Color(0xFFF97316)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(mod.description, fontSize = 13.sp, color = Color(0xFFC9D1D9))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF161B22))
                .padding(16.dp)
                .weight(1f)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Описание", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8B949E))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    mod.body.ifEmpty { mod.description },
                    fontSize = 13.sp,
                    color = Color(0xFFC9D1D9),
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (mod.installed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF34D399).copy(alpha = 0.15f))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("\u2714 Уже установлен", fontSize = 15.sp, color = Color(0xFF34D399), fontWeight = FontWeight.SemiBold)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDownloading) Color(0xFF30363D) else primaryColor)
                    .clickable(enabled = !isDownloading) {
                        isDownloading = true
                        downloadStatus = "Скачивание..."
                        scope.launch(Dispatchers.IO) {
                            val success = when (mod.source) {
                                ModSource.MODRINTH -> {
                                    val api = ModrinthApi()
                                    api.downloadModById(mod.id, mcVersion, loader, modsDir)
                                }
                                ModSource.CURSEFORGE -> {
                                    val api = CurseForgeApi()
                                    api.downloadMod(mod.id, mcVersion, loader, modsDir)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                if (success) {
                                    downloadStatus = "Установлен!"
                                    onInstalled(mod.slug)
                                } else {
                                    downloadStatus = "Ошибка скачивания"
                                }
                                isDownloading = false
                            }
                        }
                    }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isDownloading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(downloadStatus, fontSize = 15.sp, color = Color.White)
                    }
                } else {
                    Text("\u2B07 Установить", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ModIcon(url: String, name: String) {
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                val conn = java.net.URL(url).openConnection()
                conn.connectTimeout = 5000
                imageBytes = conn.getInputStream().readBytes()
            } catch (_: Exception) {}
        }
    }

    if (imageBytes != null) {
        val imageBitmap = imageBytes!!.inputStream().use { loadImageBitmap(it) }
        Image(
            bitmap = imageBitmap,
            contentDescription = name,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF30363D)),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1).uppercase(), fontSize = 16.sp, color = Color(0xFF8B949E))
        }
    }
}

@Composable
private fun ModIconLarge(url: String, name: String) {
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                val conn = java.net.URL(url).openConnection()
                conn.connectTimeout = 5000
                imageBytes = conn.getInputStream().readBytes()
            } catch (_: Exception) {}
        }
    }

    if (imageBytes != null) {
        val imageBitmap = imageBytes!!.inputStream().use { loadImageBitmap(it) }
        Image(
            bitmap = imageBitmap,
            contentDescription = name,
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF30363D)),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1).uppercase(), fontSize = 28.sp, color = Color(0xFF8B949E))
        }
    }
}

private fun formatDownloads(count: Long): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000 -> "${count / 1_000}K"
        else -> count.toString()
    }
}
