package net.bullmc.client

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.bullmc.client.api.NewsItem
import net.bullmc.client.api.ServerApi
import net.bullmc.client.api.ServerStatus
import net.bullmc.client.core.auth.Auth
import net.bullmc.client.core.auth.MicrosoftAuth
import net.bullmc.client.core.launcher.Launcher
import net.bullmc.client.core.loader.LoaderType
import net.bullmc.client.core.profile.GameProfile
import net.bullmc.client.core.profile.ProfileManager
import net.bullmc.client.core.mod.ModUpdateChecker
import net.bullmc.client.core.util.*
import net.bullmc.client.theme.ThemeManager
import net.bullmc.client.theme.ThemeName
import net.bullmc.client.ui.component.*
import net.bullmc.client.ui.screen.*
import java.awt.Desktop
import java.io.File

private const val LAUNCHER_VERSION = "1.0.0"

fun main() = application {
    LauncherPaths.init()
    ProfileManager.init()
    LauncherPaths.writeLog("Launcher started. user.home=${System.getProperty("user.home")}")

    val windowState = rememberWindowState(
        width = 1050.dp,
        height = 680.dp,
        position = WindowPosition.Aligned(androidx.compose.ui.Alignment.Center)
    )

    val auth = remember { Auth() }
    val launcher = remember { Launcher() }
    val microsoftAuth = remember { MicrosoftAuth().also { it.init() } }
    val coroutineScope = rememberCoroutineScope()

    // Discord RPC
    LaunchedEffect(Unit) {
        try { DiscordManager.init() } catch (_: Exception) {}
    }

    // Auto-download Java if missing
    var javaReady by remember { mutableStateOf(false) }
    var javaDownloadMsg by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val bundled = JavaDownloader.findJavaInDir(LauncherPaths.jre)
            if (bundled != null) {
                javaReady = true
            } else {
                val saved = auth.getJavaPath()
                if (saved != "java" && File(saved).exists()) {
                    javaReady = true
                } else {
                    javaDownloadMsg = "Скачивание Java 21..."
                    val downloaded = JavaDownloader.downloadJava(LauncherPaths.jre) { msg, _ ->
                        javaDownloadMsg = msg
                    }
                    if (downloaded != null) {
                        auth.saveJavaPath(downloaded)
                        javaReady = true
                        javaDownloadMsg = "Java установлена!"
                    } else {
                        javaDownloadMsg = "Java не найдена. Установите вручную."
                        javaReady = true
                    }
                }
            }
        }
    }

    // Загружаем сохраненную тему
    val savedThemeName = remember { auth.getTheme() }
    var currentTheme by remember { mutableStateOf(
        try {
            ThemeName.valueOf(savedThemeName)
        } catch (e: Exception) {
            ThemeName.DARK
        }
    ) }

    var savedNick by remember { mutableStateOf(auth.getPlayerNick() ?: "") }
    var playerProfiles by remember { mutableStateOf(auth.getProfiles()) }
    var defaultServer by remember { mutableStateOf(auth.getDefaultServer()) }
    var launchState by remember { mutableStateOf("READY") }
    var statusMessage by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0f) }
    var news by remember { mutableStateOf(listOf<NewsItem>()) }

    var selectedVersion by remember { mutableStateOf("1.20.4") }
    var availableVersions by remember { mutableStateOf(listOf("1.20.4")) }

    var currentScreen by remember { mutableStateOf("HOME") }
    var logLines by remember { mutableStateOf(listOf<String>()) }
    var gameProcess by remember { mutableStateOf<Process?>(null) }
    var isGameRunning by remember { mutableStateOf(false) }
    
    // Консоль логов (F8)
    var showConsole by remember { mutableStateOf(false) }
    
    // Обновление
    var updateUrl by remember { mutableStateOf<String?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    var ramMb by remember { mutableStateOf(auth.getRamMb()) }
    var javaPath by remember { mutableStateOf(auth.getJavaPath()) }
    var gameDir by remember { mutableStateOf("") }
    var modsDir by remember { mutableStateOf("") }

    var serverStatuses by remember { mutableStateOf<Map<String, ServerStatus>>(emptyMap()) }

    val serverIps = listOf("play.bullmc.net", "yt.bullmc.net")

    var selectedLoader by remember { mutableStateOf(net.bullmc.client.core.loader.LoaderType.VANILLA) }
    var selectedLoaderVersion by remember { mutableStateOf("") }
    var enabledMods by remember { mutableStateOf(listOf<String>()) }

    // Profile state
    var profiles by remember { mutableStateOf(ProfileManager.getProfiles()) }
    var activeProfile by remember { mutableStateOf(ProfileManager.getActiveProfile()) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Sync profile state on load
    LaunchedEffect(Unit) {
        selectedVersion = activeProfile.mcVersion
        selectedLoader = activeProfile.loaderType
        selectedLoaderVersion = activeProfile.loaderVersion
        enabledMods = activeProfile.enabledMods
        ramMb = activeProfile.ramMb
        gameDir = activeProfile.getGameDir().absolutePath
        modsDir = activeProfile.getModsDir().absolutePath
    }

    LaunchedEffect(Unit) {
        launch {
            news = ServerApi.getNews()
            
            val updateDownloadUrl = ServerApi.checkLauncherUpdate("1.0")
            if (updateDownloadUrl != null) {
                updateUrl = updateDownloadUrl
                showUpdateDialog = true
            }
            
            val versions = launcher.getAvailableVersions()
            if (versions.isNotEmpty()) {
                availableVersions = versions.map { it.id }
                selectedVersion = versions.first().id
            }
            gameDir = activeProfile.getGameDir().absolutePath
            modsDir = activeProfile.getModsDir().absolutePath

            serverIps.forEach { ip ->
                val status = ServerApi.getServerStatus(ip)
                serverStatuses = serverStatuses + (ip to status)
            }

            // Auto-check mod updates
            val modsDirFile = File(modsDir)
            if (modsDirFile.exists() && modsDirFile.listFiles()?.isNotEmpty() == true) {
                withContext(Dispatchers.IO) {
                    val updates = ModUpdateChecker.checkForUpdates(modsDirFile, selectedVersion, selectedLoader)
                    if (updates.isNotEmpty()) {
                        logLines = logLines + "[UPDATE] Найдено ${updates.size} обновлений модов"
                        updates.forEach { update ->
                            logLines = logLines + "[UPDATE] ${update.name}: ${update.currentVersion} -> ${update.latestVersion}"
                        }
                    }
                }
            }

            // Set Discord idle
            try { DiscordManager.setIdle() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(gameProcess) {
        val proc = gameProcess ?: return@LaunchedEffect
        isGameRunning = true
        launchState = "RUNNING"
        statusMessage = "Игра запущена"
        try { DiscordManager.setPlaying(selectedVersion, defaultServer) } catch (_: Exception) {}
        launch {
            withContext(Dispatchers.Main) {
                logLines = logLines + "[LAUNCHER] Процесс PID=${proc.pid()}, isAlive=${proc.isAlive}"
            }
            try {
                withContext(Dispatchers.IO) {
                    val reader = proc.inputStream.bufferedReader()
                    var line = reader.readLine()
                    while (line != null) {
                        withContext(Dispatchers.Main) {
                            logLines = logLines + line
                        }
                        line = reader.readLine()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    logLines = logLines + "[LAUNCHER] Ошибка чтения: ${e.message}"
                }
            }
            val exitCode = try { proc.exitValue() } catch (_: Exception) { -1 }
            withContext(Dispatchers.Main) {
                logLines = logLines + "[LAUNCHER] Процесс завершён, exitCode=$exitCode"
                isGameRunning = false
                launchState = "READY"
                gameProcess = null

                if (exitCode != 0) {
                    val gameDirFile = File(gameDir)
                    val crash = CrashAnalyzer.analyzeLog(gameDirFile)
                    if (crash != null) {
                        statusMessage = "Краш: ${crash.summary}"
                        logLines = logLines + "[CRASH] ${crash.cause}"
                        logLines = logLines + "[CRASH] Решение: ${crash.suggestion}"
                    } else {
                        statusMessage = "Игра завершена с ошибкой (код: $exitCode)"
                    }
                } else {
                    statusMessage = "Игра завершена"
                }
                try { DiscordManager.setIdle() } catch (_: Exception) {}
            }
        }
    }

    Window(
        onCloseRequest = {
            try { DiscordManager.shutdown() } catch (_: Exception) {}
            exitApplication()
        },
        state = windowState,
        title = "BullMC Client",
        resizable = false,
    ) {
        MaterialTheme(colors = ThemeManager.getColors(currentTheme)) {
            Row(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background).onKeyEvent { event ->
                    if (event.key == Key.F8) {
                        showConsole = !showConsole
                        true
                    } else {
                        false
                    }
                }
            ) {
                // Update dialog
                if (showUpdateDialog && updateUrl != null) {
                    AlertDialog(
                        onDismissRequest = { showUpdateDialog = false },
                        buttons = {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { showUpdateDialog = false },
                                    colors = androidx.compose.material.ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF21262D)
                                    )
                                ) {
                                    Text("Позже", color = Color(0xFF8B949E), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        try {
                                            java.awt.Desktop.getDesktop().browse(java.net.URI(updateUrl))
                                        } catch (e: Exception) {
                                            println("Ошибка открытия ссылки: ${e.message}")
                                        }
                                        showUpdateDialog = false
                                    },
                                    colors = androidx.compose.material.ButtonDefaults.buttonColors(
                                        backgroundColor = ThemeManager.getPrimaryColor(currentTheme)
                                    )
                                ) {
                                    Text("Скачать", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        },
                        title = { Text("Доступно обновление!", color = Color(0xFFC9D1D9), fontSize = 16.sp) },
                        text = { Text("Нажмите 'Скачать' для обновления лаунчера на новую версию.", color = Color(0xFF8B949E), fontSize = 13.sp) },
                        backgroundColor = Color(0xFF161B22)
                    )
                }
                
                Sidebar(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> currentScreen = screen },
                    primaryColor = ThemeManager.getPrimaryColor(currentTheme)
                )

                Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                                    slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                        }
                    ) { screen ->
                        Column(modifier = Modifier.fillMaxSize()) {
                            when (screen) {
                            "HOME" -> {
                                TopBanner(
                                savedNick = savedNick,
                                launchState = launchState,
                                statusMessage = statusMessage,
                                progress = progress,
                                selectedVersion = selectedVersion,
                                versions = availableVersions,
                                serverStatuses = serverStatuses,
                                primaryColor = ThemeManager.getPrimaryColor(currentTheme),
                                profiles = playerProfiles,
                                onNickChanged = { newNick ->
                                    savedNick = newNick
                                    auth.setPlayerNickFromProfile(newNick)
                                },
                                onVersionSelected = { ver ->
                                    selectedVersion = ver
                                    val newName = "${selectedLoader.displayName} $ver"
                                    ProfileManager.updateProfile(activeProfile.id) {
                                        mcVersion = ver
                                        name = newName
                                    }
                                    activeProfile = ProfileManager.getActiveProfile()
                                    profiles = ProfileManager.getProfiles()
                                },
                                activeProfileName = activeProfile.name,
                                selectedLoader = selectedLoader,
                                onLaunch = { nick ->
                                    savedNick = nick
                                    auth.savePlayerNick(nick)
                                    auth.saveSettings(ramMb, javaPath)
                                    ProfileManager.updateProfile(activeProfile.id) {
                                        mcVersion = selectedVersion
                                        loaderType = selectedLoader
                                        loaderVersion = selectedLoaderVersion
                                        enabledMods = enabledMods
                                        this.ramMb = ramMb
                                        serverIp = defaultServer
                                    }
                                    launchState = "DOWNLOADING"
                                    statusMessage = "Подготовка..."
                                    progress = 0f
                                    logLines = emptyList()
                                    try { DiscordManager.setDownloading("Скачивание $selectedVersion") } catch (_: Exception) {}

                                    coroutineScope.launch {
                                        try {
                                            logLines = logLines + "[LAUNCHER] Запуск: version=$selectedVersion, loader=$selectedLoader, nick=$nick"
                                            val process = launcher.downloadAndLaunch(
                                                version = selectedVersion,
                                                playerNick = nick,
                                                javaPath = javaPath,
                                                ramMb = ramMb,
                                                serverIp = defaultServer,
                                                loader = selectedLoader,
                                                loaderVersion = selectedLoaderVersion,
                                                enabledModIds = enabledMods,
                                                gameDir = activeProfile.getGameDir(),
                                                onStatus = { msg, prog ->
                                                    statusMessage = msg
                                                    progress = prog
                                                    if (msg.isNotEmpty()) logLines = logLines + "[LAUNCHER] $msg"
                                                }
                                            )
                                            if (process != null) {
                                                logLines = logLines + "[LAUNCHER] Процесс создан, PID: ${process.pid()}"
                                                launchState = "LAUNCHING"
                                                statusMessage = "Запуск..."
                                                gameProcess = process
                                            } else {
                                                launchState = "READY"
                                                statusMessage = "Ошибка запуска"
                                                logLines = logLines + "[LAUNCHER] Ошибка: process == null"
                                            }
                                        } catch (e: Exception) {
                                            logLines = logLines + "[LAUNCHER] Исключение: ${e.message}"
                                            launchState = "READY"
                                            statusMessage = "Ошибка: ${e.message}"
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            BottomCards(news = news, serverStatuses = serverStatuses, primaryColor = ThemeManager.getPrimaryColor(currentTheme))
                        }

                        "MODS" -> {
                            LoaderScreen(
                                selectedLoader = selectedLoader,
                                onLoaderChanged = { loader ->
                                    selectedLoader = loader
                                    val newName = "${loader.displayName} $selectedVersion"
                                    ProfileManager.updateProfile(activeProfile.id) {
                                        loaderType = loader
                                        name = newName
                                    }
                                    activeProfile = ProfileManager.getActiveProfile()
                                    profiles = ProfileManager.getProfiles()
                                },
                                selectedLoaderVersion = selectedLoaderVersion,
                                onLoaderVersionChanged = { ver ->
                                    selectedLoaderVersion = ver
                                    ProfileManager.updateProfile(activeProfile.id) { loaderVersion = ver }
                                },
                                enabledMods = enabledMods,
                                onModsChanged = { mods ->
                                    enabledMods = mods
                                    ProfileManager.updateProfile(activeProfile.id) { enabledMods = mods }
                                },
                                primaryColor = ThemeManager.getPrimaryColor(currentTheme),
                                selectedMcVersion = selectedVersion,
                                modsDir = activeProfile.getModsDir()
                            )
                        }

                        "SETTINGS" -> {
                            SettingsScreen(
                                ramMb = ramMb,
                                onRamChanged = { ramMb = it },
                                javaPath = javaPath,
                                onJavaPathChanged = { newPath ->
                                    javaPath = newPath
                                    auth.saveJavaPath(newPath)
                                },
                                gameDir = gameDir,
                                modsDir = modsDir,
                                currentTheme = currentTheme,
                                onThemeChanged = { newTheme ->
                                    currentTheme = newTheme
                                    auth.saveTheme(newTheme.name)
                                },
                                primaryColor = ThemeManager.getPrimaryColor(currentTheme),
                                defaultServer = defaultServer,
                                onDefaultServerChanged = { newServer ->
                                    defaultServer = newServer
                                    auth.setDefaultServer(newServer)
                                },
                                onOpenModsFolder = {
                                    try {
                                        val dir = File(modsDir)
                                        if (!dir.exists()) dir.mkdirs()
                                        Desktop.getDesktop().open(dir)
                                    } catch (e: Exception) {
                                        println("Ошибка открытия папки модов: ${e.message}")
                                    }
                                },
                                profiles = profiles,
                                activeProfileId = activeProfile.id,
                                onProfileSelected = { id ->
                                    ProfileManager.setActiveProfile(id)
                                    activeProfile = ProfileManager.getActiveProfile()
                                    selectedVersion = activeProfile.mcVersion
                                    selectedLoader = activeProfile.loaderType
                                    selectedLoaderVersion = activeProfile.loaderVersion
                                    enabledMods = activeProfile.enabledMods
                                    ramMb = activeProfile.ramMb
                                    gameDir = activeProfile.getGameDir().absolutePath
                                    modsDir = activeProfile.getModsDir().absolutePath
                                },
                                onProfileCreate = { name ->
                                    val newProfile = GameProfile(name = name)
                                    ProfileManager.createProfile(newProfile)
                                    ProfileManager.setActiveProfile(newProfile.id)
                                    profiles = ProfileManager.getProfiles()
                                    activeProfile = newProfile
                                    selectedVersion = newProfile.mcVersion
                                    selectedLoader = newProfile.loaderType
                                    selectedLoaderVersion = newProfile.loaderVersion
                                    enabledMods = newProfile.enabledMods
                                    gameDir = newProfile.getGameDir().absolutePath
                                    modsDir = newProfile.getModsDir().absolutePath
                                },
                                onProfileDelete = { id ->
                                    ProfileManager.deleteProfile(id)
                                    profiles = ProfileManager.getProfiles()
                                    activeProfile = ProfileManager.getActiveProfile()
                                    selectedVersion = activeProfile.mcVersion
                                    selectedLoader = activeProfile.loaderType
                                    selectedLoaderVersion = activeProfile.loaderVersion
                                    enabledMods = activeProfile.enabledMods
                                    gameDir = activeProfile.getGameDir().absolutePath
                                    modsDir = activeProfile.getModsDir().absolutePath
                                },
                                onProfileRename = { id, newName ->
                                    ProfileManager.updateProfile(id) { name = newName }
                                    profiles = ProfileManager.getProfiles()
                                    activeProfile = ProfileManager.getActiveProfile()
                                }
                            )
                        }

                        "LOGS" -> {
                            LogScreen(logLines = logLines, isGameRunning = isGameRunning, primaryColor = ThemeManager.getPrimaryColor(currentTheme))
                        }

                        "SERVERS" -> {
                            val defaultServers = listOf("play.bullmc.net", "yt.bullmc.net")
                            var savedServerList by remember {
                                val loaded = auth.getSavedServers().ifEmpty { defaultServers }
                                if (loaded != auth.getSavedServers()) auth.saveServers(loaded)
                                mutableStateOf(loaded)
                            }
                            ServerBrowserScreen(
                                primaryColor = ThemeManager.getPrimaryColor(currentTheme),
                                savedServers = savedServerList,
                                onAddServer = { ip ->
                                    savedServerList = savedServerList + ip
                                    auth.saveServers(savedServerList)
                                },
                                onRemoveServer = { ip ->
                                    savedServerList = savedServerList - ip
                                    auth.saveServers(savedServerList)
                                },
                                onJoinServer = { ip ->
                                    defaultServer = ip
                                    auth.setDefaultServer(ip)
                                    currentScreen = "HOME"
                                }
                            )
                        }

                        "ACCOUNT" -> {
                            AuthScreen(
                                microsoftAuth = microsoftAuth,
                                primaryColor = ThemeManager.getPrimaryColor(currentTheme),
                                onAuthComplete = { name ->
                                    if (name.isNotEmpty()) {
                                        savedNick = name
                                        auth.savePlayerNick(name)
                                    }
                                    currentScreen = "HOME"
                                }
                            )
                        }
                            }
                        }
                    }
                }

                if (currentScreen == "HOME") {
                    FriendsPanel(primaryColor = ThemeManager.getPrimaryColor(currentTheme))
                }
            }
            
            // Консоль логов (F8)
            if (showConsole) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xAA000000)).clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
                ) {
                    Box(
                        modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                            .fillMaxWidth(0.95f)
                            .fillMaxHeight(0.4f)
                            .background(Color(0xFF0D1117), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Game Logs (F8 для закрытия)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC9D1D9))
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF161B22))
                                        .clickable { logLines = emptyList() }.padding(6.dp)
                                ) {
                                    Text("Clear", fontSize = 9.sp, color = Color(0xFF6E7681))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                state = rememberLazyListState()
                            ) {
                                items(logLines) { line ->
                                    Text(
                                        line,
                                        fontSize = 9.sp,
                                        color = Color(0xFF6E7681),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
