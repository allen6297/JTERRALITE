package com.terralite.launcher

import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.Flow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF0D1117)
private val Surface = Color(0xFF161B22)
private val Accent = Color(0xFF58A6FF)
private val TextPrimary = Color(0xFFE6EDF3)
private val TextMuted = Color(0xFF8B949E)
private val ButtonClient = Color(0xFF238636)
private val ButtonServer = Color(0xFF1F6FEB)

data class PackDisplayInfo(
    val name: String,
    val version: String,
    val description: String
)

data class UserProfile(
    val name: String,
    val status: String = "Online"
)

data class Friend(
    val name: String,
    val status: String,
    val isOnline: Boolean
)

enum class LauncherTab {
    PLAY, CONTENT, EDITOR
}

@Composable
fun LauncherScreen(
    packsPath: String,
    packs: List<PackDisplayInfo>,
    contentRepository: ContentRepository,
    onLaunchClient: (serverAddress: String?) -> Unit,
    onLaunchServer: () -> Unit,
    onLaunchEditor: () -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }
    var showSocial by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(LauncherTab.PLAY) }
    var userProfile by remember { mutableStateOf(UserProfile("Explorer_" + (1000..9999).random())) }
    var serverAddress by remember { mutableStateOf("") }
    
    val downloadablePacks by contentRepository.getDownloadablePacks().collectAsState(emptyList())

    // Initial data seed if empty
    LaunchedEffect(Unit) {
        if (downloadablePacks.isEmpty()) {
            contentRepository.insertPack(DownloadablePack("Aether Highlands", "SkyStudio", "240 MB", "Vast floating islands and gravity-defying physics."))
            contentRepository.insertPack(DownloadablePack("Deep Oceans", "HydroDev", "185 MB", "Explore the lightless depths of the world's oceans."))
            contentRepository.insertPack(DownloadablePack("Neon Districts", "CyberCore", "412 MB", "Cyberpunk themed structures and advanced machinery."))
            contentRepository.insertPack(DownloadablePack("Medieval Siege", "HistoryBuff", "95 MB", "Castles, knights, and historical warfare mechanics."))
        }
    }
    
    val friends = remember {
        listOf(
            Friend("VoxelMaster", "Playing Terralite", true),
            Friend("PixelArtisan", "Online", true),
            Friend("DeepMiner", "Offline", false)
        )
    }

    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1117), Color(0xFF0A1628))
                )
            )
    ) {
        // Subtle background decoration
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(400.dp)
                .offset(x = 100.dp, y = (-100).dp)
                .background(Brush.radialGradient(listOf(Accent.copy(alpha = 0.05f), Color.Transparent)))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // Main Content Area
            this@Row.AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = tween(1000)
                ),
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar: (Terralite) (Tabs) (Friends/Account)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. Terralite (Logo + Brand)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TerraliteLogo(Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "TERRALITE",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 2.sp
                            )
                        }

                        // 2. Tabs
                        TabRow(
                            selectedTabIndex = selectedTab.ordinal,
                            backgroundColor = Color.Transparent,
                            contentColor = Accent,
                            divider = {},
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                                    color = Accent,
                                    height = 2.dp
                                )
                            },
                            modifier = Modifier.width(360.dp)
                        ) {
                            LauncherTab.entries.forEach { tab ->
                                Tab(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    text = {
                                        Text(
                                            tab.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    },
                                    selectedContentColor = Accent,
                                    unselectedContentColor = TextMuted
                                )
                            }
                        }

                        // 3. Friends/Account
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileHeader(userProfile)
                            Spacer(Modifier.width(12.dp))
                            IconButton(onClick = { showSocial = !showSocial }) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(if (showSocial) Accent else TextMuted, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (showSocial) "X" else "P",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.05f))

                    // Content Area
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 64.dp, bottom = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // Secondary Hero section (Optional, keeping the tagline)
                            Text(
                                text = "A platform ecosystem, not a monolithic game",
                                fontSize = 13.sp,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )

                            Spacer(Modifier.height(48.dp))

                            // Tab Content
                            Box(
                                modifier = Modifier.heightIn(min = 300.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                when (selectedTab) {
                                    LauncherTab.PLAY -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(24.dp)
                                        ) {
                                            // Server address field for joining multiplayer
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    "Server Address (leave blank for single-player)",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                                OutlinedTextField(
                                                    value = serverAddress,
                                                    onValueChange = { serverAddress = it },
                                                    placeholder = { Text("localhost:25565", color = TextMuted, fontSize = 13.sp) },
                                                    singleLine = true,
                                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                                        textColor = TextPrimary,
                                                        focusedBorderColor = Accent,
                                                        unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                                                        backgroundColor = Surface,
                                                        cursorColor = Accent
                                                    ),
                                                    modifier = Modifier.width(300.dp)
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                LaunchButton(
                                                    label = if (serverAddress.isBlank()) "Play" else "Join",
                                                    description = if (serverAddress.isBlank()) "Start single-player" else "Join $serverAddress",
                                                    color = ButtonClient,
                                                    onClick = { onLaunchClient(serverAddress.takeIf { it.isNotBlank() }) }
                                                )
                                                LaunchButton(
                                                    label = "Host Server",
                                                    description = "Run a headless server",
                                                    color = ButtonServer,
                                                    onClick = onLaunchServer
                                                )
                                            }

                                            PackInfo(packsPath = packsPath, packs = packs)
                                        }
                                    }
                                    LauncherTab.CONTENT -> {
                                        ContentBrowser(downloadablePacks)
                                    }
                                    LauncherTab.EDITOR -> {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            LaunchButton(
                                                label = "Open Editor",
                                                description = "Create and modify world content",
                                                color = Accent,
                                                onClick = onLaunchEditor
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(32.dp))

                            TextButton(onClick = { /* TODO: Settings */ }) {
                                Text("SETTINGS", fontSize = 12.sp, color = TextMuted, letterSpacing = 2.sp)
                            }
                        }
                    }
                }
            }

            // Social Sidebar
            this@Row.AnimatedVisibility(
                visible = showSocial,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it })
            ) {
                SocialSidebar(friends)
            }
        }
    }
}

@Composable
private fun ProfileHeader(profile: UserProfile) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.End) {
            Text(profile.name, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(profile.status, fontSize = 11.sp, color = Accent)
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Surface, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(profile.name.take(1), color = Accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SocialSidebar(friends: List<Friend>) {
    Surface(
        modifier = Modifier.width(280.dp).fillMaxHeight(),
        color = Surface,
        elevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "FRIENDS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 2.sp
            )
            
            Spacer(Modifier.height(24.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                friends.forEach { friend ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Background, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(friend.name.take(1), fontSize = 12.sp, color = TextMuted)
                            // Online indicator
                            if (friend.isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .align(Alignment.BottomEnd)
                                        .background(Color(0xFF238636), RoundedCornerShape(5.dp))
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(friend.name, fontSize = 14.sp, color = TextPrimary)
                            Text(friend.status, fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            }
            
            Spacer(Modifier.weight(1f))
            
            OutlinedButton(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Accent)
            ) {
                Text("ADD FRIEND", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TerraliteLogo(modifier: Modifier = Modifier.size(80.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Accent.copy(alpha = 0.2f), Color.Transparent),
                center = Offset(w / 2f, h / 2f),
                radius = w / 2f
            )
        )

        // Geometric mountain shape
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.25f)
            lineTo(w * 0.85f, h * 0.75f)
            lineTo(w * 0.15f, h * 0.75f)
            close()
        }
        drawPath(path, color = Accent)

        // Highlight
        val highlight = Path().apply {
            moveTo(w * 0.5f, h * 0.25f)
            lineTo(w * 0.65f, h * 0.75f)
            lineTo(w * 0.35f, h * 0.75f)
            close()
        }
        drawPath(highlight, color = Color.White.copy(alpha = 0.2f))
    }
}

@Composable
private fun LaunchButton(label: String, description: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(
            onClick = onClick,
            modifier = Modifier.width(180.dp).height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = color, contentColor = Color.White),
            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        ) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(description, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
private fun PackInfo(packsPath: String, packs: List<PackDisplayInfo>) {
    Surface(
        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(0.8f),
        shape = RoundedCornerShape(8.dp),
        color = Surface,
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Content Packs", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    Text(
                        text = "${packs.size} loaded",
                        fontSize = 12.sp,
                        color = if (packs.isNotEmpty()) Accent else TextMuted
                    )
                }
                Text(
                    text = packsPath,
                    fontSize = 10.sp,
                    color = TextMuted.copy(alpha = 0.7f),
                    textAlign = TextAlign.Start
                )
            }

            if (packs.isNotEmpty()) {
                Divider(color = Color.White.copy(alpha = 0.1f))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    packs.forEach { pack ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pack.name, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                if (pack.description.isNotBlank()) {
                                    Text(pack.description, fontSize = 11.sp, color = TextMuted, maxLines = 1)
                                }
                            }
                            Text(
                                text = "v${pack.version}",
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentBrowser(downloadablePacks: List<DownloadablePack>) {
    Column(
        modifier = Modifier.widthIn(max = 800.dp).fillMaxWidth(0.9f),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Search and Filters placeholder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Search content...", color = TextMuted, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("Latest", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // Content List
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            downloadablePacks.forEach { pack ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Surface,
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(pack.name, fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text("by ${pack.author}", fontSize = 12.sp, color = Accent)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(pack.description, fontSize = 13.sp, color = TextMuted)
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(pack.size, fontSize = 11.sp, color = TextMuted)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { /* TODO: Download */ },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Accent, contentColor = Color.White),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("DOWNLOAD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun LauncherScreenPreview() {
    val mockRepository = object : ContentRepository {
        override fun getDownloadablePacks(): Flow<List<DownloadablePack>> = kotlinx.coroutines.flow.flowOf(
            listOf(
                DownloadablePack("Mock Pack 1", "Author A", "10 MB", "Description A"),
                DownloadablePack("Mock Pack 2", "Author B", "20 MB", "Description B")
            )
        )
        override suspend fun insertPack(pack: DownloadablePack) {}
        override suspend fun clearPacks() {}
    }

    MaterialTheme {
        Box(modifier = Modifier.size(1280.dp, 720.dp).background(Background)) {
            LauncherScreen(
                packsPath = "/home/user/terralite/packs",
                packs = listOf(
                    PackDisplayInfo("Terralite Base", "1.0.0", "Core game content and mechanics"),
                    PackDisplayInfo("Industrial Tech", "0.5.2", "Adds machines and automation")
                ),
                contentRepository = mockRepository,
                onLaunchClient = { _ -> },
                onLaunchServer = {}
            )
        }
    }
}
