package net.bullmc.client.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.bullmc.client.core.loader.LoaderType
import net.bullmc.client.core.mod.BrowserMod
import net.bullmc.client.core.mod.CurseForgeApi
import net.bullmc.client.core.mod.ModSource
import net.bullmc.client.core.mod.ModrinthApi
import net.bullmc.client.theme.BullColors
import net.bullmc.client.theme.LocalBullColors
import net.bullmc.client.ui.component.BullCard
import net.bullmc.client.ui.component.BullChip
import net.bullmc.client.ui.component.BullPrimaryButton
import net.bullmc.client.ui.component.BullSecondaryButton
import net.bullmc.client.ui.component.BullTextField
import net.bullmc.client.ui.component.VersionPickerDialog
import net.bullmc.client.ui.state.VersionState
import java.io.File

@Composable
fun ModBrowserScreen(
    primaryColor: Color,
    versionState: VersionState,
    loader: LoaderType,
    installedModSlugs: List<String>,
    modsDir: File,
    onModInstalled: (String) -> Unit
) {
    val colors = LocalBullColors.current
    val modrinthApi = remember { ModrinthApi() }
    val curseForgeApi = remember { CurseForgeApi() }

    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<BrowserMod>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedSource by remember { mutableStateOf(ModSource.MODRINTH) }
    var selectedMod by remember { mutableStateOf<BrowserMod?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    var showSourceMenu by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedSort by remember { mutableStateOf("relevance") }
    var showVersionPicker by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            kotlinx.coroutines.delay(500)
            debouncedQuery = searchQuery
        } else {
            debouncedQuery = ""
            searchResults = emptyList()
        }
    }

    LaunchedEffect(debouncedQuery, selectedSource, versionState.selectedVersion, selectedCategory, selectedSort) {
        if (debouncedQuery.isNotBlank()) {
            isLoading = true
            statusMessage = "Поиск в ${selectedSource.displayName}..."
            withContext(Dispatchers.IO) {
                val results = when (selectedSource) {
                    ModSource.MODRINTH -> modrinthApi.searchMods(
                        debouncedQuery, versionState.selectedVersion, loader, category = selectedCategory, sort = selectedSort
                    )
                    ModSource.CURSEFORGE -> curseForgeApi.searchMods(
                        debouncedQuery, versionState.selectedVersion, loader, category = selectedCategory, sort = selectedSort
                    )
                }
                withContext(Dispatchers.Main) {
                    searchResults = results.map { mod -> mod.copy(installed = mod.slug in installedModSlugs) }
                    isLoading = false
                    statusMessage = if (results.isEmpty()) "Ничего не найдено" else ""
                }
            }
        }
    }

    val categories = listOf(
        "all" to "★ Все",
        "optimization" to "⚡ Оптимизация",
        "rendering" to "🎨 Рендеринг",
        "utility" to "🔧 Утилиты",
        "technology" to "⚙️ Технологии",
        "adventure" to "⚔️ Приключения",
        "magic" to "✨ Магия",
        "storage" to "📦 Хранилище",
        "farming" to "🌾 Фермерство",
        "decoration" to "🏠 Декор",
        "mobs" to "🐾 Мобы",
        "food" to "🍜 Еда",
        "library" to "📚 Библиотеки",
        "worldgen" to "🌍 Генерация мира",
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Магазин модов", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Text("Скачивайте моды с Modrinth и CurseForge", fontSize = 14.sp, color = colors.textMuted, modifier = Modifier.padding(top = 2.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                BullSecondaryButton(
                    text = selectedSource.displayName + " ▾",
                    onClick = { showSourceMenu = true },
                    colors = colors,
                    height = 46.dp,
                    accent = true
                )
                DropdownMenu(
                    expanded = showSourceMenu,
                    onDismissRequest = { showSourceMenu = false },
                    modifier = Modifier.background(colors.surface)
                ) {
                    DropdownMenuItem(onClick = {
                        selectedSource = ModSource.MODRINTH
                        showSourceMenu = false
                        searchResults = emptyList()
                    }) {
                        Text("Modrinth", color = colors.success)
                    }
                    DropdownMenuItem(onClick = {
                        selectedSource = ModSource.CURSEFORGE
                        showSourceMenu = false
                        searchResults = emptyList()
                    }) {
                        Text("CurseForge", color = colors.warning)
                    }
                }
            }

            BullSecondaryButton(
                text = "MC ${versionState.selectedVersion} ▾",
                onClick = { showVersionPicker = true },
                colors = colors,
                height = 46.dp
            )

            BullTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "🔍 Поиск модов...",
                modifier = Modifier.weight(1f),
                colors = colors
            )
        }

        if (statusMessage.isNotEmpty() && !isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(statusMessage, fontSize = 13.sp, color = colors.textSecondary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
            items(categories.size) { index ->
                val (catId, catLabel) = categories[index]
                val isSelected = if (catId == "all") selectedCategory == null else selectedCategory == catId
                BullChip(
                    text = catLabel,
                    selected = isSelected,
                    onClick = { selectedCategory = if (catId == "all") null else catId },
                    colors = colors
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Сортировка:", fontSize = 12.sp, color = colors.textMuted)
            val sortOptions = listOf(
                "relevance" to "По релевантности",
                "downloads" to "По популярности",
                "newest" to "По дате"
            )
            for ((sortId, sortLabel) in sortOptions) {
                BullChip(text = sortLabel, selected = selectedSort == sortId, onClick = { selectedSort = sortId }, colors = colors)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedMod != null) {
            ModDetailPanel(
                mod = selectedMod!!,
                colors = colors,
                mcVersion = versionState.selectedVersion,
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
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = colors.primary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(statusMessage, fontSize = 14.sp, color = colors.textSecondary)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(searchResults) { mod ->
                    ModListItem(
                        mod = mod,
                        colors = colors,
                        isInstalled = mod.slug in installedModSlugs,
                        onClick = { selectedMod = mod }
                    )
                }
            }
        }
    }

    if (showVersionPicker) {
        VersionPickerDialog(
            versionState = versionState,
            onDismiss = { showVersionPicker = false },
            onSelect = { versionState.selectVersion(it) },
            colors = colors
        )
    }
}

@Composable
private fun ModListItem(mod: BrowserMod, colors: BullColors, isInstalled: Boolean, onClick: () -> Unit) {
    BullCard(
        modifier = Modifier.fillMaxWidth(),
        colors = colors,
        background = if (isInstalled) colors.primary.copy(alpha = 0.08f) else colors.surface,
        hoverable = true,
        onClick = onClick,
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (mod.iconUrl != null) {
                ModIcon(mod.iconUrl, mod.name, colors)
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        mod.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isInstalled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("✔", fontSize = 12.sp, color = colors.success)
                    }
                }
                Text(
                    mod.description,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (mod.author.isNotEmpty()) {
                        Text(mod.author, fontSize = 11.sp, color = colors.textMuted)
                    }
                    Text("${formatDownloads(mod.downloads)} загрузок", fontSize = 11.sp, color = colors.textMuted)
                    Text(
                        if (mod.source == ModSource.MODRINTH) "Modrinth" else "CurseForge",
                        fontSize = 11.sp,
                        color = if (mod.source == ModSource.MODRINTH) colors.success else colors.warning
                    )
                }
            }
        }
    }
}

@Composable
private fun ModDetailPanel(
    mod: BrowserMod,
    colors: BullColors,
    mcVersion: String,
    loader: LoaderType,
    modsDir: File,
    onBack: () -> Unit,
    onInstalled: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var downloadStatus by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        BullSecondaryButton(text = "← Назад к списку", onClick = onBack, colors = colors, accent = true)
        Spacer(modifier = Modifier.height(12.dp))

        BullCard(modifier = Modifier.fillMaxWidth(), colors = colors, contentPadding = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (mod.iconUrl != null) {
                    ModIconLarge(mod.iconUrl, mod.name, colors)
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(mod.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (mod.author.isNotEmpty()) {
                            Text("by ${mod.author}", fontSize = 13.sp, color = colors.textSecondary)
                        }
                        Text("${formatDownloads(mod.downloads)} загрузок", fontSize = 13.sp, color = colors.textSecondary)
                        Text(
                            if (mod.source == ModSource.MODRINTH) "Modrinth" else "CurseForge",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (mod.source == ModSource.MODRINTH) colors.success else colors.warning
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(mod.description, fontSize = 13.sp, color = colors.textPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        BullCard(modifier = Modifier.fillMaxWidth().weight(1f), colors = colors, contentPadding = PaddingValues(16.dp)) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Описание", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    mod.body.ifEmpty { mod.description },
                    fontSize = 13.sp,
                    color = colors.textPrimary,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (mod.installed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.success.copy(alpha = 0.15f))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("✔ Уже установлен", fontSize = 15.sp, color = colors.success, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDownloading) colors.borderStrong else colors.primary)
                    .clickable(enabled = !isDownloading) {
                        isDownloading = true
                        downloadStatus = "Скачивание..."
                        scope.launch(Dispatchers.IO) {
                            val success = when (mod.source) {
                                ModSource.MODRINTH -> ModrinthApi().downloadModById(mod.id, mcVersion, loader, modsDir)
                                ModSource.CURSEFORGE -> CurseForgeApi().downloadMod(mod.id, mcVersion, loader, modsDir)
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
                    Text("⬇ Установить", fontSize = 15.sp, color = colors.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ModIcon(url: String, name: String, colors: BullColors) {
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
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)).background(colors.surfaceSunken),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1).uppercase(), fontSize = 16.sp, color = colors.textSecondary)
        }
    }
}

@Composable
private fun ModIconLarge(url: String, name: String, colors: BullColors) {
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
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).background(colors.surfaceSunken),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1).uppercase(), fontSize = 28.sp, color = colors.textSecondary)
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
