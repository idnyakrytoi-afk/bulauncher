package net.bullmc.client.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.bullmc.client.AnimDurations
import net.bullmc.client.AnimatedEntry
import net.bullmc.client.AnimatedProgressBar
import net.bullmc.client.PulsingDot
import net.bullmc.client.ShimmerBox
import net.bullmc.client.core.builds.BuildInstallResult
import net.bullmc.client.core.builds.BuildsRepository
import net.bullmc.client.core.builds.CommunityBuild
import net.bullmc.client.core.loader.LoaderType
import net.bullmc.client.core.profile.GameProfile
import net.bullmc.client.theme.BullColors
import net.bullmc.client.theme.LocalBullColors
import net.bullmc.client.theme.ThemeName
import net.bullmc.client.ui.component.BullBadge
import net.bullmc.client.ui.component.BullCard
import net.bullmc.client.ui.component.BullChip
import net.bullmc.client.ui.component.BullPrimaryButton
import net.bullmc.client.ui.component.BullSecondaryButton
import net.bullmc.client.ui.component.BullTextField

// primaryColor / currentTheme сохранены в сигнатуре ради совместимости со всеми
// вызовами: палитра и тема приходят через LocalBullColors.
@Suppress("UNUSED_PARAMETER")
@Composable
fun BuildsScreen(
    primaryColor: Color,
    currentTheme: ThemeName = ThemeName.DARK,
    activeProfile: GameProfile,
    onProfileApplied: () -> Unit
) {
    val colors = LocalBullColors.current
    val scope = rememberCoroutineScope()

    var builds by remember { mutableStateOf(BuildsRepository.getAll()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var selectedLoader by remember { mutableStateOf<LoaderType?>(null) }

    var installingId by remember { mutableStateOf<String?>(null) }
    var installStatus by remember { mutableStateOf("") }
    var installProgress by remember { mutableStateOf(0f) }

    var showPublishDialog by remember { mutableStateOf(false) }
    var showLoaderMenu by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) { BuildsRepository.refreshRemote() }
        builds = BuildsRepository.getAll()
        isLoading = false
    }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3500)
            message = null
        }
    }

    val filtered = remember(builds, searchQuery, selectedTag, selectedLoader) {
        val q = searchQuery.trim().lowercase()
        builds.filter { build ->
            val matchesQuery = q.isEmpty() ||
                    build.name.lowercase().contains(q) ||
                    build.author.lowercase().contains(q) ||
                    build.description.lowercase().contains(q) ||
                    build.mods.any { it.lowercase().contains(q) }
            val matchesTag = selectedTag == null || build.tags.any { it.equals(selectedTag, true) }
            val matchesLoader = selectedLoader == null || build.loader == selectedLoader
            matchesQuery && matchesTag && matchesLoader
        }
    }
    val tags = remember(builds) { builds.flatMap { it.tags }.distinct().sorted() }

    fun refresh() { builds = BuildsRepository.getAll() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedEntry(index = 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Сборки игроков", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text(
                            "Готовые наборы модов от сообщества — ставятся в один клик",
                            fontSize = 14.sp, color = colors.textMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    BullSecondaryButton(
                        text = "+ Опубликовать",
                        onClick = { showPublishDialog = true },
                        colors = colors,
                        accent = true,
                        enabled = installingId == null,
                        height = 42.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedEntry(index = 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BullTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Поиск сборки, автора или мода...",
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )

                    Box {
                        BullSecondaryButton(
                            text = (selectedLoader?.displayName ?: "Все лоадеры") + " ▾",
                            onClick = { showLoaderMenu = true },
                            colors = colors,
                            accent = selectedLoader != null
                        )
                        DropdownMenu(
                            expanded = showLoaderMenu,
                            onDismissRequest = { showLoaderMenu = false },
                            modifier = Modifier.background(colors.surface)
                        ) {
                            DropdownMenuItem(onClick = { selectedLoader = null; showLoaderMenu = false }) {
                                Text("Все лоадеры", fontSize = 13.sp, color = colors.textPrimary)
                            }
                            LoaderType.values().forEach { loader ->
                                DropdownMenuItem(onClick = { selectedLoader = loader; showLoaderMenu = false }) {
                                    Text(loader.displayName, fontSize = 13.sp, color = colors.textPrimary)
                                }
                            }
                        }
                    }

                    BullSecondaryButton(
                        text = "⟳",
                        onClick = {
                            isLoading = true
                            scope.launch {
                                withContext(Dispatchers.IO) { BuildsRepository.refreshRemote() }
                                refresh()
                                isLoading = false
                            }
                        },
                        colors = colors,
                        enabled = !isLoading && installingId == null
                    )
                }
            }

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                AnimatedEntry(index = 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BullChip(text = "Все", selected = selectedTag == null, onClick = { selectedTag = null }, colors = colors)
                        tags.take(6).forEach { tag ->
                            BullChip(
                                text = tag,
                                selected = selectedTag == tag,
                                onClick = { selectedTag = if (selectedTag == tag) null else tag },
                                colors = colors
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = installingId != null,
                enter = fadeIn(tween(AnimDurations.NORMAL)),
                exit = fadeOut(tween(AnimDurations.NORMAL))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(installStatus, fontSize = 12.sp, color = colors.primary)
                        Text("${(installProgress * 100).toInt()}%", fontSize = 12.sp, color = colors.textMuted)
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    AnimatedProgressBar(progress = installProgress, color = colors.primary, backgroundColor = colors.surfaceSunken)
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    isLoading && builds.isEmpty() -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            repeat(4) {
                                ShimmerBox(
                                    modifier = Modifier.fillMaxWidth().height(96.dp),
                                    baseColor = colors.surface,
                                    highlightColor = colors.surfaceHover,
                                    corner = 14.dp
                                )
                            }
                        }
                    }
                    filtered.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Ничего не найдено", fontSize = 16.sp, color = colors.textSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Измените фильтры или опубликуйте свою сборку", fontSize = 13.sp, color = colors.textMuted)
                            }
                        }
                    }
                    else -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filtered, key = { it.id }) { build ->
                                BuildCard(
                                    build = build,
                                    colors = colors,
                                    isInstalling = installingId == build.id,
                                    anyInstalling = installingId != null,
                                    onInstall = {
                                        installingId = build.id
                                        installProgress = 0f
                                        installStatus = "Установка ${build.name}..."
                                        scope.launch {
                                            val result = BuildInstallerUi.install(build) { msg, prog ->
                                                installStatus = msg
                                                installProgress = prog
                                            }
                                            refresh()
                                            installingId = null
                                            when (result) {
                                                is BuildInstallResult.Success -> {
                                                    message = if (result.failed.isEmpty()) {
                                                        "«${build.name}» установлена: ${result.installed} модов"
                                                    } else {
                                                        "Установлено ${result.installed}, не найдено: ${result.failed.joinToString(", ")}"
                                                    }
                                                    onProfileApplied()
                                                }
                                                is BuildInstallResult.Error -> message = "Ошибка: ${result.message}"
                                            }
                                        }
                                    },
                                    onExport = {
                                        val file = java.io.File(
                                            net.bullmc.client.core.util.LauncherPaths.root,
                                            "exports/${build.name.replace(Regex("[^A-Za-z0-9А-Яа-яЁё -]"), "")}.bullbuild"
                                        )
                                        BuildsRepository.exportToFile(build, file)
                                        message = "Файл сохранён: ${file.absolutePath}"
                                    },
                                    onDelete = {
                                        if (BuildsRepository.delete(build.id)) {
                                            refresh()
                                            message = "Сборка удалена"
                                        } else {
                                            message = "Можно удалять только свои сборки"
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = message != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
            enter = fadeIn(tween(AnimDurations.NORMAL)) +
                    slideInVertically(tween(AnimDurations.SLOW, easing = FastOutSlowInEasing)) { it / 2 },
            exit = fadeOut(tween(AnimDurations.NORMAL))
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceHover)
                    .border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(message ?: "", fontSize = 13.sp, color = colors.textPrimary)
            }
        }

        if (showPublishDialog) {
            PublishBuildDialog(
                colors = colors,
                activeProfile = activeProfile,
                onDismiss = { showPublishDialog = false },
                onPublish = { build ->
                    BuildsRepository.publish(build)
                    refresh()
                    showPublishDialog = false
                    message = "Сборка «${build.name}» сохранена"
                }
            )
        }
    }
}

/** Небольшая обёртка, чтобы экран не зависел от suspend-контекста напрямую. */
private object BuildInstallerUi {
    suspend fun install(build: CommunityBuild, onProgress: (String, Float) -> Unit): BuildInstallResult =
        withContext(Dispatchers.IO) {
            net.bullmc.client.core.builds.BuildInstaller.install(build, onProgress)
        }
}

@Composable
private fun BuildCard(
    build: CommunityBuild,
    colors: BullColors,
    isInstalling: Boolean,
    anyInstalling: Boolean,
    onInstall: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    BullCard(
        modifier = Modifier.fillMaxWidth(),
        colors = colors,
        hoverable = true,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                BullBadge(text = build.loader.displayName, color = colors.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    build.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (build.local) {
                    Spacer(modifier = Modifier.width(8.dp))
                    BullBadge(text = "моя", color = colors.success)
                }
            }
            Text("⬇ ${build.downloads}", fontSize = 12.sp, color = colors.textMuted)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            build.description.ifEmpty { "Без описания" },
            fontSize = 13.sp,
            color = colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (build.mods.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                build.mods.take(5).forEach { mod ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceSunken)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(mod, fontSize = 11.sp, color = colors.textSecondary)
                    }
                }
                if (build.mods.size > 5) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceSunken)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("+${build.mods.size - 5}", fontSize = 11.sp, color = colors.textMuted)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingDot(color = colors.primary.copy(alpha = 0.7f), size = 7.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(build.author, fontSize = 12.sp, color = colors.textSecondary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(build.shortInfo(), fontSize = 12.sp, color = colors.textMuted)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (build.local) {
                    BullSecondaryButton(text = "Экспорт", onClick = onExport, colors = colors, enabled = !anyInstalling)
                    BullSecondaryButton(text = "✕", onClick = onDelete, colors = colors, enabled = !anyInstalling)
                }
                if (isInstalling) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.primary, strokeWidth = 2.dp)
                } else {
                    BullSecondaryButton(text = "▶ Установить", onClick = onInstall, colors = colors, accent = true, enabled = !anyInstalling)
                }
            }
        }
    }
}

@Composable
private fun PublishBuildDialog(
    colors: BullColors,
    activeProfile: GameProfile,
    onDismiss: () -> Unit,
    onPublish: (CommunityBuild) -> Unit
) {
    var name by remember { mutableStateOf(activeProfile.name) }
    var description by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var useCurrentProfile by remember { mutableStateOf(true) }
    var mcVersion by remember { mutableStateOf(activeProfile.mcVersion) }
    var loader by remember { mutableStateOf(activeProfile.loaderType) }
    var modsText by remember { mutableStateOf(activeProfile.enabledMods.joinToString(", ")) }
    var showLoaderMenu by remember { mutableStateOf(false) }

    val author = remember {
        net.bullmc.client.core.auth.Auth().getPlayerNick()?.takeIf { it.isNotBlank() } ?: "Игрок"
    }
    val canPublish = name.isNotBlank() && mcVersion.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB0000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedEntry(index = 0, offsetY = 24f) {
            BullCard(
                modifier = Modifier.width(480.dp).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { },
                colors = colors,
                contentPadding = PaddingValues(20.dp)
            ) {
                Text("Публикация сборки", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(
                    "Сборка сохранится в каталоге и файлом .bullbuild — им можно поделиться",
                    fontSize = 12.sp, color = colors.textMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                DialogField("Название", name, colors) { name = it }
                DialogField("Описание", description, colors) { description = it }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        DialogField("Версия MC", mcVersion, colors) { mcVersion = it }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        Column {
                            Text("Лоадер", fontSize = 11.sp, color = colors.textMuted, modifier = Modifier.padding(bottom = 4.dp))
                            Box {
                                BullSecondaryButton(
                                    text = loader.displayName + " ▾",
                                    onClick = { showLoaderMenu = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = colors,
                                    height = 42.dp
                                )
                                DropdownMenu(
                                    expanded = showLoaderMenu,
                                    onDismissRequest = { showLoaderMenu = false },
                                    modifier = Modifier.background(colors.surface)
                                ) {
                                    LoaderType.values().forEach { type ->
                                        DropdownMenuItem(onClick = { loader = type; showLoaderMenu = false }) {
                                            Text(type.displayName, fontSize = 13.sp, color = colors.textPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                DialogField("Моды (через запятую)", modsText, colors) { modsText = it }
                DialogField("Теги (через запятую)", tagsText, colors) { tagsText = it }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BullSecondaryButton(text = "Отмена", onClick = onDismiss, colors = colors)
                    Spacer(modifier = Modifier.width(8.dp))
                    BullPrimaryButton(
                        text = "Опубликовать",
                        onClick = {
                            val mods = modsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val tags = tagsText.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                            onPublish(
                                CommunityBuild(
                                    name = name.trim(),
                                    author = author,
                                    description = description.trim(),
                                    mcVersion = mcVersion.trim(),
                                    loader = loader,
                                    loaderVersion = if (useCurrentProfile) activeProfile.loaderVersion else "",
                                    mods = mods,
                                    tags = tags,
                                    ramMb = activeProfile.ramMb,
                                    local = true
                                )
                            )
                        },
                        colors = colors,
                        enabled = canPublish,
                        height = 42.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogField(label: String, value: String, colors: BullColors, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Text(label, fontSize = 11.sp, color = colors.textMuted, modifier = Modifier.padding(bottom = 4.dp))
        BullTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = "...",
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            fontSize = 13
        )
    }
}
