package com.petpal.app.ui.battle

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petpal.app.domain.battle.*
import com.petpal.app.domain.model.Mood
import com.petpal.app.domain.platform.LinkKind
import com.petpal.app.ui.components.NeuButton
import com.petpal.app.ui.components.PetSprite
import com.petpal.app.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import com.petpal.app.domain.platform.SoundPlayer
import androidx.compose.ui.geometry.Rect
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BattleScreen(vm: BattleViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    // A stray back swipe mid-fight must not drop the match; once it's over (or before it starts) back leaves.
    BattleBackHandler(enabled = s.mode != BattleScreenMode.LOBBY) {
        if (s.mode != BattleScreenMode.FIGHT || s.fight?.result != null) vm.leave()
    }
    when (s.mode) {
        BattleScreenMode.LOBBY -> Lobby(vm, s.message, s.format)
        BattleScreenMode.HOSTING -> Waiting(
            if (s.format.guests > 1) "Waiting for ${s.format.guests} friends via ${s.link?.label}..."
            else "Waiting for an opponent via ${s.link?.label}...",
            listOfNotNull(s.message, hostHint(s.link)).joinToString("\n\n"), vm::leave,
        )
        BattleScreenMode.CONNECTING -> Waiting("Connecting to ${s.message}...", null, vm::leave)
        BattleScreenMode.SEARCHING -> Searching(s, vm)
        BattleScreenMode.ROOM -> s.room?.let { Room(it, s.link, vm) }
            ?: Waiting("Opening the room...", hostHint(s.link), vm::leave)
        BattleScreenMode.FIGHT -> s.fight?.let { Fight(it, vm) }
        BattleScreenMode.MATCHMAKING -> Waiting("Finding an opponent online...",
            "Ranked 1v1 against a random player. If nobody's been online for a while the server needs up to a minute to wake up.", vm::leave)
        BattleScreenMode.LEADERBOARD -> LeaderboardScreen(s, vm)
    }
}

private fun hostHint(kind: LinkKind?) = when (kind) {
    LinkKind.HOTSPOT -> "Turn on your phone's hotspot (or join the same Wi-Fi). Your friend connects to it and taps Join."
    null -> null
}

@Composable
private fun Lobby(vm: BattleViewModel, message: String?, format: BattleFormat) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Battle", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Fight a friend nearby with no internet, or a random player online. Pets roam on their own. Time your moves for when they're in range. Dealing and taking hits charges your energy bar; a full bar unleashes your unique (slot 5).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val bossOpen by vm.bossOpen.collectAsStateWithLifecycle()
        if (bossOpen) BossCard(vm)
        if (vm.hasOnline) OnlineCard(vm)
        FormatPicker(format, vm::setFormat)
        LinkKind.entries.forEach { kind -> LinkCard(kind, vm) }
        NeuButton(outlined = true,
            onClick = vm::practice,
            icon = Icons.Rounded.SmartToy,
            text = when (format) {
                BattleFormat.SOLO -> "Practice vs CPU"
                BattleFormat.TEAMS -> "3v3 vs CPUs (with CPU partners)"
                BattleFormat.FFA -> "Free-for-all vs 3 CPUs"
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Online battles: win +45 XP, draw +25, loss +15 (first 3 per day). CPU: win +28. Stronger pets come from walking.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BossCard(vm: BattleViewModel) {
    ElevatedCard(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Event: Boss battle", style = MaterialTheme.typography.titleMedium)
            Text(
                "A random Lv ${com.petpal.app.domain.logic.Events.BOSS_LEVEL} pet with " +
                    "+${com.petpal.app.domain.logic.Events.BOSS_BONUS_HP} HP. Beat it once to unlock a random pet. " +
                    "Retry as often as you like until the end of November.",
                style = MaterialTheme.typography.bodySmall,
            )
            NeuButton(outlined = true, onClick = vm::boss, icon = Icons.Rounded.EmojiEvents, text = "Fight the boss",
                filled = true, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun FormatPicker(format: BattleFormat, onPick: (BattleFormat) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Format", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeuButton(outlined = true, onClick = { onPick(BattleFormat.SOLO) }, text = "1v1", icon = Icons.Rounded.Person,
                selected = format == BattleFormat.SOLO, modifier = Modifier.weight(1f))
            NeuButton(outlined = true, onClick = { onPick(BattleFormat.TEAMS) }, text = "Teams", icon = Icons.Rounded.Diversity3,
                selected = format == BattleFormat.TEAMS, modifier = Modifier.weight(1f))
            NeuButton(outlined = true, onClick = { onPick(BattleFormat.FFA) }, text = "FFA", icon = Icons.Rounded.EmojiEvents,
                selected = format == BattleFormat.FFA, modifier = Modifier.weight(1f))
        }
        if (format != BattleFormat.SOLO) Text(
            when (format) {
                BattleFormat.TEAMS -> "Up to $MAX_PLAYERS players join your room and pick a team. You start when both teams have someone."
                else -> "Up to $MAX_PLAYERS players join your room, everyone for themselves. Last pet standing wins."
            } + " The host picks the format.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Waiting room for team battles and free-for-alls. */
@Composable
private fun Room(r: RoomUi, link: LinkKind?, vm: BattleViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (r.ffa) "Free-for-all room" else "Team battle room",
                style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Text("${r.players.size}/$MAX_PLAYERS", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            when {
                r.host -> (hostHint(link)?.let { "$it " } ?: "") +
                    if (r.ffa) "Tap Start when everyone is in." else "Tap a player to move them to the other team."
                r.ffa -> "Waiting for the host to start..."
                else -> "Waiting for the host to start. You can switch teams until then."
            },
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        @Composable
        fun PlayerRow(i: Int, color: Color) {
            val p = r.players[i]
            val tag = when { i == r.you -> " (you)"; i == 0 -> " (host)"; else -> "" }
            ListItem(
                headlineContent = { Text(p.name + tag) },
                supportingContent = { Text("Lv ${p.level}") },
                leadingContent = { Icon(Icons.Rounded.Pets, null, tint = color) },
                trailingContent = if (r.host && !r.ffa) { { Icon(Icons.Rounded.SwapHoriz, "Switch team") } } else null,
                modifier = Modifier.clip(MaterialTheme.shapes.medium)
                    .then(if (r.host && !r.ffa) Modifier.clickable { vm.switchTeam(i) } else Modifier),
            )
        }
        if (r.ffa) {
            r.players.indices.forEach { PlayerRow(it, MaterialTheme.colorScheme.primary) }
        } else {
            listOf(0 to "Team A", 1 to "Team B").forEach { (t, label) ->
                val color = if (t == 0) AllyBlue else MaterialTheme.colorScheme.error
                Text(label, style = MaterialTheme.typography.titleMedium, color = color)
                val members = r.players.indices.filter { r.players[it].team == t }
                if (members.isEmpty()) Text("Nobody yet", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                members.forEach { PlayerRow(it, color) }
            }
            if (!r.host) NeuButton(outlined = true, onClick = { vm.switchTeam(r.you) }, text = "Switch team", icon = Icons.Rounded.SwapHoriz,
                modifier = Modifier.fillMaxWidth())
        }
        if (r.host) NeuButton(outlined = true,
            onClick = vm::startRoom, enabled = r.canStart, filled = true, icon = Icons.Rounded.PlayArrow,
            text = when {
                r.canStart -> "Start (${r.players.size} players)"
                r.ffa -> "Need at least 2 players"
                else -> "Need someone on each team"
            },
            modifier = Modifier.fillMaxWidth(),
        )
        NeuButton(outlined = true, onClick = vm::leave, text = if (r.host) "Close room" else "Leave", icon = Icons.Rounded.Close,
            modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun LinkCard(kind: LinkKind, vm: BattleViewModel) {
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val ask = rememberLinkPermission(kind) { ok -> if (ok) pending?.invoke(); pending = null }
    val available = remember(kind) { vm.isAvailable(kind) }
    val surface = MaterialTheme.colorScheme.surfaceVariant
    Card(colors = CardDefaults.cardColors(containerColor = surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WifiTethering, null)
                Spacer(Modifier.width(10.dp))
                Text(kind.label, style = MaterialTheme.typography.titleMedium)
            }
            if (!available) {
                Text("Not available on this device", style = MaterialTheme.typography.bodySmall)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NeuButton(outlined = true,
                        onClick = { pending = { vm.host(kind) }; ask() },
                        text = "Host", icon = Icons.Rounded.Podcasts, filled = true, surface = surface,
                        modifier = Modifier.weight(1f),
                    )
                    NeuButton(outlined = true,
                        onClick = { pending = { vm.search(kind) }; ask() },
                        text = "Join", icon = Icons.Rounded.Search, surface = surface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineCard(vm: BattleViewModel) {
    val surface = MaterialTheme.colorScheme.surfaceVariant
    Card(colors = CardDefaults.cardColors(containerColor = surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Public, null)
                Spacer(Modifier.width(10.dp))
                Text("Online ranked", style = MaterialTheme.typography.titleMedium)
            }
            Text("1v1 against a random player. Wins raise your rating; the top 10 make the leaderboard. On the 1st of every month, #1 gets a free pet.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NeuButton(outlined = true, onClick = vm::findOnline, text = "Find match", icon = Icons.Rounded.TravelExplore,
                    filled = true, surface = surface, modifier = Modifier.weight(1f))
                NeuButton(outlined = true, onClick = vm::showLeaderboard, text = "Top 10", icon = Icons.Rounded.Leaderboard,
                    surface = surface, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LeaderboardScreen(s: BattleUiState, vm: BattleViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Leaderboard", style = MaterialTheme.typography.headlineMedium)
        val board = s.board
        when {
            s.boardError != null -> Text(s.boardError, color = MaterialTheme.colorScheme.error)
            board == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Loading (the server may take a minute to wake up)...", style = MaterialTheme.typography.bodyMedium)
            }
            board.top.isEmpty() -> Text("No ranked matches yet. Be the first!")
            else -> board.top.forEach { RankRow(it, highlight = it.rank == board.me?.rank) }
        }
        board?.me?.let { me ->
            Spacer(Modifier.height(8.dp))
            Text("You", style = MaterialTheme.typography.titleMedium)
            if (me.rank == 0) Text("Rating ${me.rating}. Finish a ranked match to get a place.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else RankRow(me, highlight = true)
        }
        Spacer(Modifier.height(8.dp))
        NeuButton(outlined = true, onClick = vm::findOnline, text = "Find match", icon = Icons.Rounded.TravelExplore, filled = true,
            modifier = Modifier.fillMaxWidth())
        NeuButton(outlined = true, onClick = vm::leave, text = "Back", icon = Icons.Rounded.Close, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RankRow(e: com.petpal.app.domain.platform.RankEntry, highlight: Boolean) {
    val medal = when (e.rank) { 1 -> Color(0xFFFFC107); 2 -> Color(0xFFB0BEC5); 3 -> Color(0xFFCD7F32); else -> null }
    ListItem(
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${e.rank}", style = MaterialTheme.typography.titleMedium, color = medal ?: MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(40.dp))
                if (e.species != null && e.stage != null) PetSprite(e.stage, Mood.HAPPY, e.species, size = 40.dp)
            }
        },
        headlineContent = { Text(e.name) },
        supportingContent = { Text("${e.wins}W ${e.losses}L" + if (e.draws > 0) " ${e.draws}D" else "") },
        trailingContent = { Text("${e.rating}", style = MaterialTheme.typography.titleMedium) },
        colors = if (highlight) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            else ListItemDefaults.colors(),
        modifier = Modifier.clip(MaterialTheme.shapes.medium),
    )
}

@Composable
private fun Waiting(title: String, hint: String?, onCancel: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator()
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        hint?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        NeuButton(outlined = true, onClick = onCancel, text = "Cancel", icon = Icons.Rounded.Close)
    }
}

@Composable
private fun Searching(s: BattleUiState, vm: BattleViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nearby hosts", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        Text(
            "Connect to your friend's hotspot or Wi-Fi first.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (s.peers.isEmpty()) Text("Searching...")
        s.peers.forEach { p ->
            ListItem(
                headlineContent = { Text(p.name) },
                leadingContent = { Icon(Icons.Rounded.PhoneAndroid, null) },
                trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable { vm.join(p) },
            )
        }
        Spacer(Modifier.weight(1f))
        NeuButton(outlined = true, onClick = vm::leave, text = "Back", icon = Icons.Rounded.Close, modifier = Modifier.fillMaxWidth())
    }
}

/** Set by the fight screen so the app hides its bottom bar and the arena gets the whole screen. */
val LocalBattleFullscreen = staticCompositionLocalOf<MutableState<Boolean>?> { null }

@Composable
private fun Fight(f: FightState, vm: BattleViewModel) {
    val fullscreen = LocalBattleFullscreen.current
    DisposableEffect(fullscreen) {
        fullscreen?.value = true
        onDispose { fullscreen?.value = false }
    }
    val sound = koinInject<SoundPlayer>()
    var soundOn by remember { mutableStateOf(sound.enabled) }
    BattleSounds(f, sound)
    val me = f.snap.fighters[f.me]
    val info = f.fighters[f.me]
    val myTeam = f.snap.fighters.indices.filter { teamOf(f, it) == teamOf(f, f.me) }.sortedBy { it != f.me }
    val rivals = f.snap.fighters.indices.filter { teamOf(f, it) != teamOf(f, f.me) }
    val crowd = f.fighters.size > 2
    Column(Modifier.fillMaxSize().background(EmberCanvas)) {
        // HUD sits above the arena (never over it) so pets near the top stay visible.
        Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { TerrainBadge(f) }
                Text("${f.snap.timeLeftMs / 1000}s", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Icon(
                    if (soundOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                    contentDescription = if (soundOn) "Mute sounds" else "Unmute sounds",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(start = 6.dp).size(28.dp).clip(CircleShape)
                        .clickable { soundOn = !soundOn; sound.enabled = soundOn }.padding(4.dp),
                )
            }
            val primary = MaterialTheme.colorScheme.primary
            @Composable fun bar(i: Int, m: Modifier) = f.fighters[i].let { o ->
                HpBar(fighterTag(f, i) + o.name, "Lv ${o.level}", f.snap.fighters[i], fighterColor(f, i, primary), crowd, m)
            }
            if (isFfa(f)) {
                // Free-for-all: everyone in a grid, two per row, you first.
                (myTeam + rivals).chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { bar(it, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else Row(Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Your team on the left (blue), theirs on the right (red).
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (crowd) Text("ALLIES", style = MaterialTheme.typography.labelSmall, color = AllyBlue)
                    myTeam.forEach { bar(it, Modifier.fillMaxWidth()) }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (crowd) Text("ENEMIES", style = MaterialTheme.typography.labelSmall, color = EnemyRed)
                    rivals.forEach { bar(it, Modifier.fillMaxWidth()) }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Arena(f, Modifier.fillMaxSize())
            if (f.log.isNotEmpty()) {
                Column(
                    Modifier.align(Alignment.BottomStart).padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.35f), MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    f.log.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = Color.White) }
                }
            }
            if (f.result != null) {
                Card(
                    Modifier.align(Alignment.Center).padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    // Arm the button after a beat so taps meant for move buttons don't skip the result.
                    var armed by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { kotlinx.coroutines.delay(1200); armed = true }
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(f.result, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        val pad = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            NeuButton(outlined = true, onClick = vm::battleAgain, modifier = Modifier.weight(1f), text = "Battle again",
                                icon = Icons.Rounded.Replay, filled = true, enabled = armed, contentPadding = pad,
                                surface = MaterialTheme.colorScheme.tertiaryContainer)
                            NeuButton(outlined = true, onClick = vm::leave, modifier = Modifier.weight(1f), text = "Back to lobby",
                                enabled = armed, contentPadding = pad, surface = MaterialTheme.colorScheme.tertiaryContainer)
                        }
                        if (f.humans.size > 1) Text(
                            (if (f.humans.size > 2) "Everyone taps" else "Both players tap") +
                                " Battle again for a rematch on the next map.",
                            style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            run {
                EnergyRow(me)
                (info.loadout.withIndex() + IndexedValue(-1, null)).chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (i, m) ->
                            if (m == null) EvadeButton(me, Modifier.weight(1f), vm::evade)
                            else MoveButton(m, me.cooldowns.getOrElse(i) { 0 }, info, me, f.terrain, Modifier.weight(1f)) { vm.use(m) }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** Current map and what it boosts; "home turf" when your pet shares the terrain's type. */
@Composable
private fun TerrainBadge(f: FightState) {
    val t = f.terrain
    val home = t.isHomeTurf(f.fighters[f.me])
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Terrain, null, Modifier.size(14.dp), tint = t.accent())
            Spacer(Modifier.width(4.dp))
            Text(
                t.label + (if (t.effectName.isNotEmpty()) " · ${t.effectName}" else "") + if (home) " · Home turf!" else "",
                style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.9f), maxLines = 1,
            )
        }
        if (t != Terrain.ARENA) Text(
            t.effect, style = MaterialTheme.typography.labelSmall, color = t.accent().copy(alpha = 0.9f), maxLines = 1,
        )
    }
}

@Composable
private fun EnergyRow(me: FighterSnap) {
    val full = me.energy >= BattleRules.MAX_ENERGY
    val energyColor = if (full) EmberGold else EmberOrange
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Bolt, null, Modifier.size(16.dp), tint = energyColor)
        Text(if (full) " FULL" else " ${me.energy}", style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(40.dp))
        LinearProgressIndicator(
            progress = { me.energy / BattleRules.MAX_ENERGY.toFloat() },
            modifier = Modifier.weight(1f).height(8.dp),
            color = energyColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun EvadeButton(me: FighterSnap, modifier: Modifier, onClick: () -> Unit) {
    val cd = me.evadeCdMs
    val enabled = cd == 0L && !me.has(Status.STUN) && !me.has(Status.ROOT) && !me.has(Status.VANISH)
    Box(
        modifier.height(64.dp).clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, ClearSky.copy(alpha = if (enabled) 1f else 0.3f), MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        if (cd > 0) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((cd.toFloat() / Evade.COOLDOWN_MS).coerceIn(0f, 1f))
                .background(Color.Black.copy(alpha = 0.18f)))
        }
        Column(
            Modifier.fillMaxSize().padding(4.dp).alpha(if (enabled) 1f else 0.45f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.KeyboardDoubleArrowUp, null, Modifier.size(20.dp).rotate(-90f), tint = ClearSky)
            Text("Evade", style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(if (cd > 0) "${(cd / 100) / 10.0}s" else "back off", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun MoveButton(m: Move, cd: Long, info: FighterInfo, me: FighterSnap, terrain: Terrain, modifier: Modifier, onClick: () -> Unit) {
    val needsEnergy = m.category == MoveCategory.UNIQUE && me.energy < BattleRules.MAX_ENERGY
    val enabled = cd == 0L && !needsEnergy && !me.has(Status.STUN)
    val accent = m.tint()
    val total = BattleStats.cooldown(info, m, terrain).coerceAtLeast(1)
    Box(
        modifier.height(64.dp).clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, m.category.color().copy(alpha = if (enabled) 1f else 0.3f), MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        if (cd > 0) { // cooldown sweep drains left to right
            Box(Modifier.fillMaxHeight().fillMaxWidth((cd.toFloat() / total).coerceIn(0f, 1f))
                .background(Color.Black.copy(alpha = 0.18f)))
        }
        Column(
            Modifier.fillMaxSize().padding(4.dp).alpha(if (enabled) 1f else 0.45f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(m.icon(), null, Modifier.size(20.dp), tint = accent)
            Text(m.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, textAlign = TextAlign.Center)
            Text(
                when {
                    cd > 0 -> "${(cd / 100) / 10.0}s"
                    needsEnergy -> "⚡${me.energy}%"
                    m.isBuff -> "buff"
                    m.damage == 0 -> "debuff"
                    else -> m.category.label.lowercase()
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HpBar(name: String, level: String, f: FighterSnap, color: Color, compact: Boolean, modifier: Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Same color as the ring and tag over this pet on the field.
            Box(Modifier.size(9.dp).background(color, CircleShape))
            Spacer(Modifier.width(4.dp))
            Text("$name  $level", style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White, modifier = Modifier.weight(1f, fill = false))
            statusIcons(f).forEach { (icon, tint) ->
                Icon(icon, null, Modifier.padding(start = 2.dp).size(if (compact) 11.dp else 14.dp), tint = tint)
            }
            if (compact) {
                Spacer(Modifier.weight(1f))
                Text("${f.hp}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            }
        }
        LinearProgressIndicator(
            progress = { f.hp.toFloat() / f.maxHp },
            modifier = Modifier.fillMaxWidth().height(if (compact) 6.dp else 10.dp),
            color = color,
            trackColor = Color.White.copy(alpha = 0.3f),
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        if (!compact) Text("${f.hp}/${f.maxHp}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
    }
}

/**
 * The floor (gradients, scenery, dozens of blotches) never changes during a fight, so paint it once
 * into a bitmap and just blit that each frame instead of redrawing everything at 60 fps.
 */
@Composable
private fun ArenaFloor(terrain: Terrain, modifier: Modifier) {
    Spacer(modifier.drawWithCache {
        val w = size.width.toInt()
        val h = size.height.toInt()
        val img = if (w > 0 && h > 0) ImageBitmap(w, h).also { bmp ->
            androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
                this, layoutDirection, androidx.compose.ui.graphics.Canvas(bmp), size,
            ) { if (terrain == Terrain.ARENA) drawArenaFloor() else drawTerrainFloor(terrain) }
        } else null
        onDrawBehind { img?.let { drawImage(it) } }
    })
}

/** Fixed (seeded) floor specks in arena units, so both phones and every frame look the same. */
private val floorSpecks by lazy {
    val r = kotlin.random.Random(42)
    List(90) { Offset(r.nextFloat(), r.nextFloat()) to (0.6f + r.nextFloat() * 1.4f) }
}

private fun DrawScope.drawArenaFloor() {
    drawRect(Brush.verticalGradient(listOf(EmberWell, EmberCanvas)))
    val cell = size.width / 8
    var gx = cell
    while (gx < size.width) { drawLine(EmberLine.copy(alpha = 0.35f), Offset(gx, 0f), Offset(gx, size.height), 1f); gx += cell }
    var gy = cell
    while (gy < size.height) { drawLine(EmberLine.copy(alpha = 0.35f), Offset(0f, gy), Offset(size.width, gy), 1f); gy += cell }
    floorSpecks.forEach { (p, rad) ->
        drawCircle(EmberCream.copy(alpha = 0.06f), rad.dp.toPx(), Offset(p.x * size.width, p.y * size.height))
    }
    val line = EmberOrange.copy(alpha = 0.28f)
    drawLine(line, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 2.dp.toPx())
    drawCircle(line, size.width * 0.16f, center, style = Stroke(2.dp.toPx()))
    drawCircle(EmberOrange.copy(alpha = 0.06f), size.width * 0.16f, center)
    drawRect(EmberOrange.copy(alpha = 0.22f), style = Stroke(3.dp.toPx()))
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
        center, radius = size.maxDimension * 0.75f))
}

/** Hit spark on a pet that just took damage. */
internal fun DrawScope.drawImpact(c: Offset, t: Float) {
    val k = 1f - t
    drawCircle(EmberCream.copy(alpha = 0.8f * k), (10 + 26 * t).dp.toPx(), c, style = Stroke((4 * k + 1).dp.toPx()))
    for (a in 0 until 8) {
        val ang = a * (PI / 4).toFloat() + 0.3f
        val d0 = (14 + 18 * t).dp.toPx()
        val d1 = d0 + 10.dp.toPx() * k
        val ux = cos(ang); val uy = sin(ang)
        drawLine(EmberGold.copy(alpha = k), Offset(c.x + ux * d0, c.y + uy * d0), Offset(c.x + ux * d1, c.y + uy * d1),
            2.5.dp.toPx(), StrokeCap.Round)
    }
}

/**
 * Draws a move exactly over its hitbox (see Hitbox.kt), in arena units scaled by [sx]/[sy]:
 * a dashed telegraph while winding up, then the effect itself, aimed at where the target was.
 */
internal fun DrawScope.drawMove(s: FighterSnap, info: FighterInfo, sx: Float, sy: Float) {
    val m = s.action ?: return
    val shape = m.shape
    val age = s.actionAgeMs
    val hitAt = m.windupMs
    if (shape == HitShape.SELF) {
        val after = age - hitAt
        val c = Offset(s.x * sx, s.y * sy)
        drawMoveFx(m, c, c, after, isSelf = true)
        if (m.effect != Effect.GUARD && after in 0..450) {
            val t = after / 450f
            for (k in 0..2) drawCircle(m.tint().copy(alpha = (1 - t) * 0.7f), (30 + 40 * t + k * 10).dp.toPx(), c,
                style = Stroke((3 - k).dp.toPx()))
        }
        return
    }
    fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
    fun circle(cx: Float, cy: Float, r: Float) = Rect(p(cx - r, cy - r), p(cx + r, cy + r))

    val range = BattleStats.range(info, m)
    val hs = m.hitSize(range)
    val winding = age < hitAt
    val ox = if (winding) s.x else s.originX
    val oy = if (winding) s.y else s.originY
    val (dx, dy) = aimDir(ox, oy, s.aimX, s.aimY, s.facing)
    val px = -dy
    val py = dx // perpendicular to the aim
    val tint = m.tint()
    val big = m.category != MoveCategory.NORMAL

    fun beamPath(len: Float, half: Float) = Path().apply {
        moveTo(p(ox + px * half, oy + py * half))
        lineTo(p(ox + dx * len + px * half, oy + dy * len + py * half))
        lineTo(p(ox + dx * len - px * half, oy + dy * len - py * half))
        lineTo(p(ox - px * half, oy - py * half))
        close()
    }
    fun conePath(len: Float) = Path().apply {
        val half = m.coneHalfAngle()
        val base = atan2(dy, dx)
        moveTo(p(ox, oy))
        val steps = 12
        for (k in 0..steps) {
            val a = base - half + 2 * half * k / steps
            lineTo(p(ox + cos(a) * len, oy + sin(a) * len))
        }
        close()
    }
    val (cx, cy) = m.hitCenter(ox, oy, dx, dy, s.aimX, s.aimY, range)

    if (winding) { // telegraph: the real hitbox, filling in as the wind-up completes
        val t = age / hitAt.toFloat().coerceAtLeast(1f)
        val path = when (shape) {
            HitShape.MELEE, HitShape.STRIKE -> Path().apply { addOval(circle(cx, cy, hs)) }
            HitShape.BEAM, HitShape.PROJECTILE -> beamPath(range, hs)
            else -> conePath(range)
        }
        if (m.explodes) path.addOval(circle(ox + dx * range, oy + dy * range, BLAST_RADIUS)) // where it blows up if nothing's in the way
        drawPath(path, tint.copy(alpha = 0.08f + 0.14f * t))
        drawPath(path, tint.copy(alpha = 0.6f), style = Stroke(1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))))
        drawMoveFx(m, p(ox, oy), p(cx, cy), age - hitAt, isSelf = false)
        return
    }

    val after = age - hitAt
    val impact = when (shape) {
        HitShape.PROJECTILE -> if (s.projectileAgeMs < 0) null
            else minOf(PROJECTILE_SPEED * s.projectileAgeMs / 1000f, range).let { p(ox + dx * it, oy + dy * it) }
        HitShape.BEAM, HitShape.CONE -> p(ox + dx * range * 0.75f, oy + dy * range * 0.75f)
        else -> p(cx, cy)
    }
    if (impact != null) drawMoveFx(m, p(ox, oy), impact, after, isSelf = false)
    when (shape) {
        HitShape.PROJECTILE -> {
            if (s.projectileAgeMs < 0) return
            val d = minOf(PROJECTILE_SPEED * s.projectileAgeMs / 1000f, range)
            val bx = ox + dx * d
            val by = oy + dy * d
            val tail = minOf(d, 0.14f)
            val from = p(bx - dx * tail, by - dy * tail)
            val spin = s.projectileAgeMs / 60f
            when (if (m.explodes) 2 else m.variant(4)) {
                1 -> { // the move's own glyph, spinning, with fading after-images
                    for (k in 3 downTo 0) {
                        val back = minOf(d, k * 0.035f)
                        drawSignature(m, p(bx - dx * back, by - dy * back), hs * sx * 0.9f, spin - k * 0.5f, 1f - k * 0.28f)
                    }
                }
                2 -> { // comet: hot head shedding sparks
                    for (k in 0 until 7) {
                        val f = ((spin * 0.35f + k / 7f) % 1f)
                        val back = minOf(d, f * 0.16f)
                        val side = sin(k * 2.3f + spin) * hs * (0.4f + f)
                        val q = p(bx - dx * back + px * side, by - dy * back + py * side)
                        drawCircle(m.coreTint().copy(alpha = 1f - f), hs * sx * 0.35f * (1f - f * 0.6f), q)
                    }
                    val glow = circle(bx, by, hs * 1.9f)
                    drawOval(Brush.radialGradient(listOf(Color.White, m.coreTint(), tint, tint.copy(alpha = 0f)), p(bx, by), hs * 1.9f * sx),
                        glow.topLeft, glow.size)
                }
                3 -> { // twin orbs braiding around the aim line
                    for (k in 0 until 10) {
                        val back = minOf(d, k * 0.014f)
                        val w = sin((d - back) * 45f) * hs * 0.9f
                        listOf(1f, -1f).forEach { sg ->
                            drawCircle(tint.copy(alpha = 0.6f * (1 - k / 10f)), hs * sx * 0.3f,
                                p(bx - dx * back + px * w * sg, by - dy * back + py * w * sg))
                        }
                    }
                    val w = sin(d * 45f) * hs * 0.9f
                    listOf(1f, -1f).forEach { sg ->
                        val q = p(bx + px * w * sg, by + py * w * sg)
                        drawCircle(Brush.radialGradient(listOf(Color.White, m.coreTint(), tint.copy(alpha = 0f)), q, hs * sx), hs * sx, q)
                    }
                }
                else -> {
                    drawLine(Brush.linearGradient(listOf(Color.Transparent, tint.copy(alpha = 0.8f)), from, p(bx, by)),
                        from, p(bx, by), hs * 1.4f * sx, StrokeCap.Round)
                    val glow = circle(bx, by, hs * 1.6f)
                    drawOval(Brush.radialGradient(listOf(Color.White, m.coreTint(), tint, tint.copy(alpha = 0f)), p(bx, by), hs * 1.6f * sx),
                        glow.topLeft, glow.size)
                }
            }
        }
        HitShape.MELEE -> {
            if (after > 260) return
            val t = after / 260f
            val k = 1f - t
            val thick = (if (big) 12 else 8).dp.toPx()
            fun stroke(path: Path) {
                drawPath(path, tint.copy(alpha = 0.55f * k), style = Stroke(thick, cap = StrokeCap.Round))
                drawPath(path, Color.White.copy(alpha = k), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            }
            when (m.variant(4)) {
                1 -> { // X cross-slash, the second stroke a beat after the first
                    val r = hs * 0.8f
                    for (n in 0..1) {
                        val g = ((t - n * 0.25f) / 0.6f).coerceIn(0f, 1f)
                        if (g <= 0f) continue
                        val sgn = if (n == 0) 1f else -1f
                        val ax = cx + (px * sgn - dx) * r; val ay = cy + (py * sgn - dy) * r
                        val bx = cx - (px * sgn - dx) * r; val by = cy - (py * sgn - dy) * r
                        stroke(Path().apply { moveTo(p(ax, ay)); lineTo(p(ax + (bx - ax) * g, ay + (by - ay) * g)) })
                    }
                }
                2 -> { // shockwave: ring and spikes bursting from the hit point
                    val c = p(cx, cy)
                    val r = hs * sx * (0.3f + 0.9f * t)
                    drawCircle(tint.copy(alpha = 0.5f * k), r, c, style = Stroke(thick * k + 1f))
                    drawCircle(Color.White.copy(alpha = 0.8f * k), r * 0.85f, c, style = Stroke(2.dp.toPx()))
                    for (a in 0 until 8) {
                        val ang = a * PI.toFloat() / 4 + m.ordinal
                        val u = Offset(cos(ang), sin(ang))
                        drawLine(tint.copy(alpha = k), c + u * r * 0.9f, c + u * r * 1.35f, 3.dp.toPx(), StrokeCap.Round)
                    }
                }
                3 -> { // three raking claw marks
                    val r = hs * 0.75f
                    for (n in -1..1) {
                        val g = ((t - (n + 1) * 0.08f) / 0.5f).coerceIn(0f, 1f)
                        if (g <= 0f) continue
                        val off = n * hs * 0.32f
                        val ax = cx + px * r + dx * off - dx * r * 0.3f; val ay = cy + py * r + dy * off - dy * r * 0.3f
                        val bx = cx - px * r + dx * off + dx * r * 0.3f; val by = cy - py * r + dy * off + dy * r * 0.3f
                        stroke(Path().apply { moveTo(p(ax, ay)); lineTo(p(ax + (bx - ax) * g, ay + (by - ay) * g)) })
                    }
                }
                else -> {
                    val slashes = if (m.effect == Effect.MULTI) 3 else if (big) 2 else 1
                    for (n in 0 until slashes) {
                        val shift = (n - (slashes - 1) / 2f) * hs * 0.45f
                        val r = hs * (0.7f + 0.3f * t)
                        stroke(Path().apply {
                            moveTo(p(cx + px * r + dx * shift, cy + py * r + dy * shift))
                            quadTo(p(cx + dx * (r * 0.9f + shift), cy + dy * (r * 0.9f + shift)),
                                p(cx - px * r + dx * shift, cy - py * r + dy * shift))
                        })
                    }
                }
            }
        }
        HitShape.BEAM -> {
            if (after > 380) return
            val k = 1f - after / 380f
            val grow = minOf(1f, after / 90f)
            val len = range * grow
            when (m.variant(3)) {
                1 -> { // helix: two strands twisting around a thin core
                    drawPath(beamPath(len, hs * 0.25f), m.coreTint().copy(alpha = 0.9f * k))
                    listOf(0f, PI.toFloat()).forEach { phase ->
                        val strand = Path()
                        val steps = 40
                        for (i in 0..steps) {
                            val f = len * i / steps
                            val w = sin(f * 40f + phase + after / 40f) * hs
                            val q = p(ox + dx * f + px * w, oy + dy * f + py * w)
                            if (i == 0) strand.moveTo(q) else strand.lineTo(q)
                        }
                        drawPath(strand, tint.copy(alpha = 0.8f * k), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                        drawPath(strand, Color.White.copy(alpha = 0.7f * k), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
                2 -> { // pulses racing down the line
                    drawPath(beamPath(len, hs * 0.6f), tint.copy(alpha = 0.3f * k))
                    val dash = hs * sx * 1.2f
                    val line = Path().apply { moveTo(p(ox, oy)); lineTo(p(ox + dx * len, oy + dy * len)) }
                    drawPath(line, m.coreTint().copy(alpha = k), style = Stroke(hs * sx * 1.2f, cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash * 0.8f), -after / 380f * dash * 12)))
                    drawPath(line, Color.White.copy(alpha = k), style = Stroke(1.5.dp.toPx()))
                }
                else -> {
                    drawPath(beamPath(len, hs), tint.copy(alpha = 0.55f * k))
                    drawPath(beamPath(len, hs * 0.45f), m.coreTint().copy(alpha = 0.9f * k))
                    drawPath(beamPath(len, hs * 0.15f), Color.White.copy(alpha = k))
                }
            }
            val ring = circle(ox, oy, hs * 1.3f)
            drawOval(Color.White.copy(alpha = 0.7f * k), ring.topLeft, ring.size)
        }
        HitShape.CONE -> {
            if (after > 420) return
            val t = after / 420f
            val k = 1f - t
            val reach = range * minOf(1f, 0.35f + t * 1.6f)
            val base = atan2(dy, dx)
            val half = m.coneHalfAngle()
            when (m.variant(3)) {
                1 -> { // streaks shooting out across the cone
                    drawPath(conePath(reach), tint.copy(alpha = 0.18f * k))
                    for (i in 0 until 11) {
                        val a = base - half + 2 * half * (i + 0.5f) / 11
                        val f0 = ((t * 1.6f + i * 0.37f) % 1f) * reach
                        val f1 = minOf(reach, f0 + range * 0.25f)
                        val u0 = p(ox + cos(a) * f0, oy + sin(a) * f0)
                        val u1 = p(ox + cos(a) * f1, oy + sin(a) * f1)
                        drawLine(tint.copy(alpha = 0.8f * k), u0, u1, 4.dp.toPx(), StrokeCap.Round)
                        drawLine(Color.White.copy(alpha = 0.7f * k), u0, u1, 1.5.dp.toPx(), StrokeCap.Round)
                    }
                }
                2 -> { // spray of the move's own particles
                    drawPath(conePath(reach), Brush.radialGradient(
                        listOf(tint.copy(alpha = 0.35f * k), tint.copy(alpha = 0f)), p(ox, oy), (reach * sx).coerceAtLeast(1f)))
                    for (i in 0 until 16) {
                        val a = base - half + 2 * half * ((i * 0.618f) % 1f)
                        val f = reach * ((0.2f + i * 0.13f + t) % 1f)
                        drawSignature(m, p(ox + cos(a) * f, oy + sin(a) * f), 4.dp.toPx(), a + t * 6, k)
                    }
                }
                else -> drawPath(conePath(reach), Brush.radialGradient(
                    listOf(m.coreTint().copy(alpha = 0.95f * k), tint.copy(alpha = 0.75f * k), tint.copy(alpha = 0.1f * k)),
                    p(ox, oy), (reach * sx).coerceAtLeast(1f),
                ))
            }
            if (m.element == Element.WATER || m.damage == 0) for (w in 1..3) { // wave fronts
                val f = (t + w * 0.22f) % 1f
                drawPath(conePath(range * f), Color.White.copy(alpha = 0.35f * k * (1 - f)), style = Stroke(2.dp.toPx()))
            }
        }
        HitShape.STRIKE -> {
            if (after > 360) return
            val k = 1f - after / 360f
            val tgt = p(cx, cy)
            val r = 7.dp.toPx()
            val join = StrokeJoin.Round
            val w = (if (big) 10 else 6).dp.toPx()
            fun bolt(x: Float, top: Float, segs: Int, amp: Float) = Path().apply {
                moveTo(x + amp, top)
                var y = top
                var left = true
                while (y < tgt.y) {
                    y = minOf(tgt.y, y + (tgt.y - top) / segs + 1f)
                    left = !left
                    lineTo(tgt.x + if (y >= tgt.y) 0f else if (left) -amp else amp, y)
                }
            }
            fun zap(path: Path, width: Float) {
                drawPath(path, tint.copy(alpha = 0.6f * k), style = Stroke(width, cap = StrokeCap.Round, join = join))
                drawPath(path, Color.White.copy(alpha = k), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = join))
            }
            when (m.variant(3)) {
                1 -> { // forked: a main bolt with two branches splitting off
                    zap(bolt(tgt.x, 0f, 6, r * 1.6f), w)
                    listOf(-1f, 1f).forEach { sg ->
                        val y0 = tgt.y * 0.45f
                        val br = Path().apply {
                            moveTo(tgt.x + sg * r, y0)
                            lineTo(tgt.x + sg * r * 3f, y0 + tgt.y * 0.12f)
                            lineTo(tgt.x + sg * r * 2.2f, y0 + tgt.y * 0.22f)
                            lineTo(tgt.x + sg * r * 4f, y0 + tgt.y * 0.35f)
                        }
                        zap(br, w * 0.5f)
                    }
                }
                2 -> { // pillar of light slamming down
                    val pw = hs * sx * 1.1f
                    drawRect(Brush.verticalGradient(listOf(tint.copy(alpha = 0f), tint.copy(alpha = 0.7f * k), Color.White.copy(alpha = k)),
                        0f, tgt.y), Offset(tgt.x - pw / 2, 0f), Size(pw, tgt.y))
                    drawRect(Color.White.copy(alpha = 0.8f * k), Offset(tgt.x - pw * 0.12f, 0f), Size(pw * 0.24f, tgt.y))
                }
                else -> zap(bolt(tgt.x, 0f, 6, r * 1.6f), w)
            }
            val c = circle(cx, cy, hs)
            drawOval(tint.copy(alpha = 0.35f * k), c.topLeft, c.size)
            drawOval(Color.White.copy(alpha = 0.8f * k), c.topLeft, c.size, style = Stroke(2.dp.toPx()))
        }
        HitShape.SELF -> Unit
    }
}

private fun Path.moveTo(o: Offset) = moveTo(o.x, o.y)
private fun Path.lineTo(o: Offset) = lineTo(o.x, o.y)
private fun Path.quadTo(c: Offset, e: Offset) = quadraticTo(c.x, c.y, e.x, e.y)

/** Hot inner color of a move's effect. */
internal fun Move.coreTint(): Color = when (element) {
    Element.FIRE -> EmberGold
    Element.ELECTRIC -> Color(0xFFFFF3A0)
    Element.WATER -> Color(0xFFBFE6FF)
    Element.ICE -> Color(0xFFE8F8FF)
    Element.GRASS -> Color(0xFFD7F5A0)
    else -> EmberCream
}

@Composable
internal fun Arena(fight: FightState, modifier: Modifier) {
    val f = fight.copy(snap = rememberSmoothSnap(fight.snap))
    val bob by rememberInfiniteTransition().animateFloat(
        0f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse),
    )
    val meColor = MaterialTheme.colorScheme.primary
    BoxWithConstraints(modifier.clipToBounds()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val sx = w / ARENA_W
        val sy = h / ARENA_H
        fun pos(s: FighterSnap) = Offset(s.x * sx, s.y * sy)
        val spriteDp = 72.dp
        val hurtboxDash = with(androidx.compose.ui.platform.LocalDensity.current) {
            remember(this) { PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())) }
        }
        ArenaFloor(f.terrain, Modifier.matchParentSize())
        Canvas(Modifier.matchParentSize()) {
            val hurtboxStroke = Stroke(2.5.dp.toPx(), pathEffect = hurtboxDash)
            f.snap.traps.forEach { tr -> drawTrap(tr, Offset(tr.x * sx, tr.y * sy), Trap.RADIUS * minOf(sx, sy)) }
            f.snap.fighters.forEachIndexed { i, s ->
                val c = pos(s)
                if (s.hp <= 0) return@forEachIndexed
                val ring = fighterColor(f, i, meColor)
                drawOval(Color.Black.copy(alpha = 0.2f), Offset(c.x - 26.dp.toPx(), c.y + 24.dp.toPx()),
                    Size(52.dp.toPx(), 12.dp.toPx()))
                // hurtbox: exactly the circle moves are tested against (yours orange, theirs red)
                val hr = Size(PET_RADIUS * sx * 2, PET_RADIUS * sy * 2)
                val tl = Offset(c.x - hr.width / 2, c.y - hr.height / 2)
                if (!s.has(Status.VANISH)) {
                    drawOval(ring.copy(alpha = 0.18f), tl, hr)
                    drawOval(ring.copy(alpha = 0.9f), tl, hr, style = hurtboxStroke)
                }
            }
            f.snap.fighters.forEachIndexed { i, s ->
                val c = pos(s)
                val info = f.fighters[i]
                val r = 38.dp.toPx()
                if (s.has(Status.VANISH)) { // shadow on the ground where it will reappear from
                    drawOval(Color.Black.copy(alpha = 0.25f), Offset(c.x - r * 0.6f, c.y + r * 0.4f), Size(r * 1.2f, r * 0.4f))
                }
                if (s.has(Status.COUNTER)) drawCircle(Color.White.copy(alpha = 0.8f), r * 1.15f, c, style = Stroke(3.dp.toPx()))
                if (s.has(Status.SHIELD)) {
                    drawCircle(Color(0xFF7FD3FF).copy(alpha = 0.18f), r * 1.1f, c)
                    drawCircle(Color(0xFF7FD3FF).copy(alpha = 0.8f), r * 1.1f, c, style = Stroke(2.5.dp.toPx()))
                }
                if (s.has(Status.MARKED)) { // crosshair over the pet
                    val mr = r * 0.55f
                    val my = c.y - r * 1.05f
                    val red = Color(0xFFE53935).copy(alpha = 0.85f)
                    drawCircle(red, mr * 0.5f, Offset(c.x, my), style = Stroke(2.dp.toPx()))
                    drawLine(red, Offset(c.x - mr * 0.7f, my), Offset(c.x + mr * 0.7f, my), 2.dp.toPx())
                    drawLine(red, Offset(c.x, my - mr * 0.7f), Offset(c.x, my + mr * 0.7f), 2.dp.toPx())
                }
                if (s.has(Status.ROOT)) drawCircle(Color(0xFF3F9A4A).copy(alpha = 0.5f), r * 0.95f, c, style = Stroke(5.dp.toPx()))
                if (s.has(Status.PARALYZE)) drawCircle(Sunflower.copy(alpha = 0.35f), r, c, style = Stroke(2.dp.toPx()))
                if (s.has(Status.GUARD)) drawCircle(ClearSky.copy(alpha = 0.7f), r, c, style = Stroke(4.dp.toPx()))
                if (s.has(Status.HARDEN)) drawCircle(Color(0xFF8D8D8D).copy(alpha = 0.6f), r * 1.05f, c, style = Stroke(6.dp.toPx()))
                if (s.has(Status.SLOW)) drawCircle(Color(0xFF7FC8E8).copy(alpha = 0.25f), r * 0.9f, c)
                if (s.has(Status.RAGE)) drawCircle(Color(0xFFD9534F).copy(alpha = 0.5f), r * 1.1f, c, style = Stroke(2.dp.toPx()))
                if (s.has(Status.HASTE)) for (k in 0..2) {
                    val y = c.y - 10.dp.toPx() + k * 10.dp.toPx()
                    val x0 = c.x - s.facing * (r + 4.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.7f), Offset(x0, y), Offset(x0 - s.facing * 14.dp.toPx(), y), 2.dp.toPx(), StrokeCap.Round)
                }
                if (s.has(Status.BURN)) for (k in 0..2) {
                    drawCircle(Color(0xFFE5532D), 3.dp.toPx(), Offset(c.x - 10.dp.toPx() + k * 10.dp.toPx(), c.y - r - (k % 2) * 4.dp.toPx()))
                }
                if (s.has(Status.STUN)) for (k in 0..2) {
                    drawCircle(Sunflower, 3.dp.toPx(), Offset(c.x - 12.dp.toPx() + k * 12.dp.toPx(), c.y - r - 6.dp.toPx()))
                }
                drawMove(s, info, sx, sy)
            }
            f.snap.fighters.forEach { s -> if (s.hurtAgeMs < 260) drawImpact(pos(s), s.hurtAgeMs / 260f) }
        }
        val density = androidx.compose.ui.platform.LocalDensity.current
        val half = with(density) { (spriteDp / 2).roundToPx() }
        f.snap.fighters.forEachIndexed { i, s ->
            val info = f.fighters[i]
            val hurt = s.hurtAgeMs < 300
            val c = pos(s)
            PetSprite(
                stage = info.stage,
                mood = if (s.hp < s.maxHp * 0.3f) Mood.SAD else Mood.HAPPY,
                species = info.species,
                size = spriteDp,
                fried = s.has(Status.FRIED),
                modifier = Modifier
                    .offset { IntOffset(c.x.roundToInt() - half, c.y.roundToInt() - half) }
                    .graphicsLayer {
                        scaleX = s.facing.toFloat()
                        alpha = when {
                            s.hp <= 0 && f.snap.fighters.size > 2 -> 0.25f // knocked out
                            s.has(Status.VANISH) -> 0f // flying high or underground
                            s.has(Status.EVADE) -> 0.5f // dashing, can't be hit
                            hurt && (s.hurtAgeMs / 75) % 2 == 0L -> 0.35f
                            else -> 1f
                        }
                    },
            )
        }
        // Smokescreen clouds drift over the pets: puffs swell in, then thin out before vanishing.
        if (f.snap.smokes.isNotEmpty()) Canvas(Modifier.matchParentSize()) {
            f.snap.smokes.forEach { sm ->
                val t = sm.ageMs / Smoke.DURATION_MS.toFloat()
                val alpha = (minOf(1f, sm.ageMs / 250f) * (1f - t * t)).coerceIn(0f, 1f)
                val c = Offset(sm.x * sx, sm.y * sy)
                val r = Smoke.RADIUS * minOf(sx, sy) * (0.7f + 0.3f * minOf(1f, sm.ageMs / 400f))
                for (k in 0 until 7) {
                    val a = k * 0.8976f + sm.ageMs / 1800f
                    val o = Offset(c.x + cos(a) * r * 0.55f, c.y + sin(a) * r * 0.4f)
                    drawCircle(Brush.radialGradient(listOf(Color(0xFF9A9A9A).copy(alpha = 0.55f * alpha), Color.Transparent), o, r * 0.75f),
                        r * 0.75f, o)
                }
                drawCircle(Color(0xFFB0B0B0).copy(alpha = 0.45f * alpha), r * 0.6f, c)
            }
        }
        // Fireball explosions: a white-hot flash that swells to the blast radius, a shockwave ring and flying embers.
        if (f.snap.blasts.isNotEmpty()) Canvas(Modifier.matchParentSize()) {
            f.snap.blasts.forEach { b -> drawBlast(Offset(b.x * sx, b.y * sy), BLAST_RADIUS * minOf(sx, sy), b.ageMs) }
        }
        // Tag over every other pet in its color: ALLY / FOE in team fights, the name in a free-for-all.
        val tagLift = with(density) { (spriteDp / 2 + 18.dp).roundToPx() }
        val tagW = with(density) { 90.dp.roundToPx() }
        f.snap.fighters.forEachIndexed { i, s ->
            if (i == f.me || s.hp <= 0 || s.has(Status.VANISH)) return@forEachIndexed
            val c = pos(s)
            val col = fighterColor(f, i, meColor)
            val label = when {
                isFfa(f) -> f.fighters[i].name
                teamOf(f, i) == teamOf(f, f.me) -> "ALLY · ${f.fighters[i].name}"
                else -> "FOE · ${f.fighters[i].name}"
            }
            Box(Modifier.width(90.dp).offset { IntOffset(c.x.roundToInt() - tagW / 2, c.y.roundToInt() - tagLift) },
                contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (col.luminance() > 0.5f) Color.Black else Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.background(col, RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 1.dp))
            }
        }
        // "YOU" marker bobbing above your pet
        val mine = pos(f.snap.fighters[f.me])
        val markerW = with(density) { 44.dp.roundToPx() }
        val lift = with(density) { (spriteDp / 2 + 30.dp + 6.dp * bob).roundToPx() }
        Column(
            Modifier.width(44.dp).offset { IntOffset(mine.x.roundToInt() - markerW / 2, mine.y.roundToInt() - lift) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("YOU", style = MaterialTheme.typography.labelSmall, color = Color.White,
                modifier = Modifier.background(meColor, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
            Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(20.dp).offset(y = (-6).dp), tint = meColor)
        }
    }
}

private val AllyBlue = Color(0xFF4FA3E0)
private val EnemyRed = Color(0xFFE74C3C)
/** Free-for-all: one distinct color per rival (no orange = you, no blue/red = team colors). */
private val FfaColors = listOf(
    Color(0xFFB26BFF), Color(0xFFFFD23F), Color(0xFF2EE6C5), Color(0xFFFF5FA2), Color(0xFF9BE15D), Color(0xFFBFBFBF),
)

private fun isFfa(f: FightState) = f.fighters.size > 2 && f.teams.toSet().size == f.fighters.size

/** You = [me]; team fights: allies blue, foes red; free-for-all: a color per player. */
private fun fighterColor(f: FightState, i: Int, me: Color): Color = when {
    i == f.me -> me
    isFfa(f) -> FfaColors[(if (i > f.me) i - 1 else i) % FfaColors.size]
    teamOf(f, i) == teamOf(f, f.me) -> AllyBlue
    else -> EnemyRed
}

private fun fighterTag(f: FightState, i: Int) = when {
    i == f.me -> "You · "
    i in f.humans -> if (isFfa(f) || teamOf(f, i) != teamOf(f, f.me)) "" else "Friend · "
    else -> ""
}

private fun teamOf(f: FightState, i: Int) = f.teams[i]

private fun statusIcons(f: FighterSnap) = buildList {
    if (f.has(Status.BURN)) add(Icons.Rounded.LocalFireDepartment to Color(0xFFE5532D))
    if (f.has(Status.SLOW)) add(Icons.Rounded.AcUnit to ClearSky)
    if (f.has(Status.STUN)) add(Icons.Rounded.Bolt to Sunflower)
    if (f.has(Status.RAGE)) add(Icons.Rounded.KeyboardDoubleArrowUp to Color(0xFFD9534F))
    if (f.has(Status.GUARD)) add(Icons.Rounded.Shield to ClearSky)
    if (f.has(Status.HASTE)) add(Icons.Rounded.Speed to Color.White)
    if (f.has(Status.HARDEN)) add(Icons.Rounded.VerifiedUser to Color(0xFFBDBDBD))
    if (f.has(Status.ROOT)) add(Icons.Rounded.Grass to Color(0xFF3F9A4A))
    if (f.has(Status.WEAKEN)) add(Icons.Rounded.ArrowDownward to Color(0xFFBDBDBD))
    if (f.has(Status.PARALYZE)) add(Icons.Rounded.FlashOn to Sunflower)
    if (f.has(Status.LEECH)) add(Icons.Rounded.Spa to Color(0xFF4CAF6A))
    if (f.has(Status.EVADE)) add(Icons.Rounded.KeyboardDoubleArrowUp to ClearSky)
    if (f.has(Status.COUNTER)) add(Icons.Rounded.Replay to Color.White)
    if (f.has(Status.SHIELD)) add(Icons.Rounded.Shield to Color(0xFF7FD3FF))
    if (f.has(Status.MARKED)) add(Icons.Rounded.GpsFixed to Color(0xFFE53935))
}

/** A snare on the floor: a faint ring while arming, then a spiky ring in the move's color. */
internal fun DrawScope.drawTrap(t: TrapSnap, c: Offset, r: Float) {
    val armed = t.ageMs >= Trap.ARM_MS
    val fade = (1f - (t.ageMs - (Trap.DURATION_MS - 600)).coerceAtLeast(0) / 600f).coerceIn(0f, 1f)
    val col = t.element.tint().copy(alpha = (if (armed) 0.85f else 0.35f) * fade)
    drawCircle(col.copy(alpha = col.alpha * 0.25f), r, c)
    drawCircle(col, r, c, style = Stroke(r * 0.18f))
    if (!armed) return
    for (k in 0 until 6) {
        val a = k * (PI.toFloat() / 3f)
        val inner = Offset(c.x + cos(a) * r * 0.45f, c.y + sin(a) * r * 0.45f)
        val outer = Offset(c.x + cos(a) * r * 0.95f, c.y + sin(a) * r * 0.95f)
        drawLine(col, inner, outer, r * 0.14f)
    }
}

fun Move.icon(): ImageVector = when (this) {
    Move.SUN_RAY, Move.SOLAR_BEAM, Move.SOLAR_FLARE -> Icons.Rounded.WbSunny
    Move.SHADOW_SWIPE, Move.SHADOW_CLAW, Move.NIGHT_TERROR -> Icons.Rounded.DarkMode
    Move.FLAME_BURST, Move.INFERNO, Move.DRAGON_FURY -> Icons.Rounded.Fireplace
    Move.MOON_HOP, Move.MOON_KICK, Move.LUNAR_STRIKE -> Icons.Rounded.Brightness3
    Move.RALLY_BARK, Move.PACK_CALL, Move.ALPHA_HOWL -> Icons.Rounded.Groups
    Move.STATIC_EYE, Move.STORM_EYE, Move.TEMPEST_EYE -> Icons.Rounded.Visibility
    Move.SHELL_UP, Move.FORTRESS, Move.CITADEL -> Icons.Rounded.Security
    Move.WISP_FLAME, Move.FOXFIRE, Move.NINE_TAILS -> Icons.Rounded.AutoAwesome
    Move.SPLASH_WAVE, Move.TIDAL_WAVE, Move.TSUNAMI -> Icons.Rounded.Waves
    Move.BEAR_SWIPE, Move.MAUL, Move.GRIZZLY_RAMPAGE -> Icons.Rounded.FrontHand
    Move.FROST_SLIDE, Move.POLAR_CRASH, Move.EMPEROR_BLIZZARD -> Icons.Rounded.AcUnit
    Move.SPIKE_ROLL, Move.NEEDLE_STORM, Move.QUILL_BARRAGE -> Icons.Rounded.Park
    Move.ECHO_SHRIEK, Move.VAMPIRE_FANG, Move.BLOOD_MOON -> Icons.Rounded.NightsStay
    Move.ANTLER_JAB, Move.VERDANT_CHARGE, Move.SPIRIT_STAMPEDE -> Icons.Rounded.Forest
    Move.PEBBLE_TOSS, Move.RIVER_RUSH, Move.RAPID_CURRENT -> Icons.Rounded.Pool
    Move.BAMBOO_SMACK, Move.BAMBOO_STORM, Move.PANDA_QUAKE -> Icons.Rounded.Spa
    Move.TAIL_LASH, Move.MAGMA_TAIL, Move.VOLCANO_ROAR -> Icons.Rounded.Volcano
    Move.SNATCH, Move.TRASH_TOSS, Move.MIDNIGHT_HEIST -> Icons.Rounded.Masks
    Move.EUCALYPTUS_DART, Move.DROWSY_SLAM, Move.DREAM_STORM -> Icons.Rounded.Bedtime
    Move.FIN_SLICE, Move.JAW_LOCK, Move.FEEDING_FRENZY -> Icons.Rounded.SetMeal
    Move.TONGUE_SNAP, Move.MIRAGE_SWAP, Move.PRISM_BLAST -> Icons.Rounded.Palette
    Move.LOG_TOSS, Move.TIMBER_TRAP, Move.GREAT_DAM -> Icons.Rounded.Carpenter
    else -> when (effect) {
        Effect.SHIELD -> Icons.Rounded.Shield
        Effect.AID -> Icons.Rounded.VolunteerActivism
        Effect.TRAP -> Icons.Rounded.Adjust
        Effect.MARK -> Icons.Rounded.GpsFixed
        Effect.SWAP -> Icons.Rounded.SyncAlt
        Effect.DISRUPT -> Icons.Rounded.HourglassBottom
        Effect.CHAIN -> Icons.Rounded.Link
        Effect.GUARD -> Icons.Rounded.Shield
        Effect.RAGE -> Icons.Rounded.Campaign
        Effect.HASTE -> Icons.Rounded.Speed
        Effect.HARDEN -> Icons.Rounded.VerifiedUser
        Effect.HEAL -> Icons.Rounded.Healing
        Effect.BATTLE_CRY -> Icons.Rounded.RecordVoiceOver
        Effect.FORTRESS -> Icons.Rounded.Security
        Effect.COUNTER -> Icons.Rounded.Replay
        Effect.BLINK -> Icons.Rounded.SwapHoriz
        Effect.CLEANSE -> Icons.Rounded.Autorenew
        Effect.ENERGIZE -> Icons.Rounded.BatteryChargingFull
        Effect.VANISH -> if (element == Element.GROUND || element == Element.WATER) Icons.Rounded.VerticalAlignBottom
            else Icons.Rounded.Flight
        Effect.WEAKEN -> Icons.Rounded.ArrowDownward
        Effect.PARALYZE -> Icons.Rounded.FlashOn
        Effect.LEECH -> Icons.Rounded.Spa
        Effect.RECHARGE -> Icons.Rounded.FlashAuto
        else -> when (element) {
            Element.NORMAL -> when (effect) {
                Effect.LUNGE -> Icons.Rounded.KeyboardDoubleArrowRight
                Effect.DRAIN -> Icons.Rounded.Restaurant
                Effect.STUN -> Icons.Rounded.Face
                Effect.KNOCKBACK -> Icons.Rounded.Moving
                else -> if (this == Move.CLAW || this == Move.FURY_SWIPES) Icons.Rounded.Pets else Icons.Rounded.SportsMma
            }
            Element.FIRE -> if (category == MoveCategory.SPECIAL) Icons.Rounded.LocalFireDepartment else Icons.Rounded.Whatshot
            Element.WATER -> if (effect == Effect.SLOW) Icons.Rounded.BubbleChart else Icons.Rounded.WaterDrop
            Element.GRASS -> if (category == MoveCategory.SPECIAL) Icons.Rounded.Grass else Icons.Rounded.Spa
            Element.ICE -> Icons.Rounded.AcUnit
            Element.ELECTRIC -> Icons.Rounded.Bolt
            Element.FLYING -> Icons.Rounded.Air
            Element.GROUND -> Icons.Rounded.Landscape
            Element.FIGHTING -> Icons.Rounded.SportsMma
        }
    }
}

/** Effect color used for buttons and arena visuals. */
fun Move.tint(): Color = when {
    this == Move.SHADOW_CLAW || this == Move.NIGHT_TERROR || this == Move.MOON_KICK || this == Move.LUNAR_STRIKE ||
        this == Move.SHADOW_SWIPE || this == Move.MOON_HOP ->
        Color(0xFF7E57C2)
    this == Move.FOXFIRE || this == Move.NINE_TAILS || this == Move.WISP_FLAME -> Color(0xFF6C8CFF)
    this == Move.ECHO_SHRIEK || this == Move.VAMPIRE_FANG || this == Move.BLOOD_MOON -> Color(0xFFC2185B)
    effect == Effect.GUARD -> ClearSky
    effect in setOf(Effect.HEAL, Effect.PACK_CALL, Effect.ALPHA, Effect.AID) -> Color(0xFF4CAF6A)
    effect == Effect.SHIELD && element == Element.NORMAL -> Color(0xFF7FD3FF)
    this == Move.LOCK_ON -> Color(0xFFE53935)
    effect == Effect.RAGE || effect == Effect.BATTLE_CRY -> Color(0xFFD9534F)
    effect == Effect.HASTE -> Color(0xFF26A69A)
    effect in setOf(Effect.HARDEN, Effect.FORTRESS, Effect.CITADEL) -> Color(0xFF8D8D8D)
    else -> element.tint()
}

fun Element.tint(): Color = when (this) {
    Element.NORMAL -> Sunflower
    Element.FIRE -> Color(0xFFE5532D)
    Element.WATER -> Color(0xFF3B8ED0)
    Element.GRASS -> Color(0xFF3F9A4A)
    Element.ICE -> Color(0xFF7FC8E8)
    Element.ELECTRIC -> Color(0xFFE8B500)
    Element.FLYING -> Color(0xFF8FB3E0)
    Element.GROUND -> Color(0xFF8B6B4A)
    Element.FIGHTING -> Color(0xFFC0503A)
}

@Composable
fun MoveCategory.color(): Color = when (this) {
    MoveCategory.NORMAL -> MaterialTheme.colorScheme.outline
    MoveCategory.SPECIAL -> EmberOrangeBright
    MoveCategory.UNIQUE -> EmberGold
}


/**
 * Snapshots arrive ~20 times a second (over the air, with jitter). Render every frame:
 * glide positions from the previous snapshot and advance animation clocks between them.
 */
@Composable
private fun rememberSmoothSnap(snap: BattleSnapshot): BattleSnapshot {
    val clock = remember { kotlin.time.TimeSource.Monotonic.markNow() }
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) withFrameMillis { now = clock.elapsedNow().inWholeMilliseconds } }
    val track = remember { SmoothTrack(snap) }
    val t = clock.elapsedNow().inWholeMilliseconds
    if (track.cur !== snap) {
        track.prev = track.render(t) // glide from where it was drawn, not where it was
        track.gap = (t - track.at).coerceIn(30, 120).let { (track.gap * 3 + it) / 4 }
        track.cur = snap
        track.at = t
    }
    return track.render(maxOf(now, t))
}

private class SmoothTrack(var cur: BattleSnapshot) {
    var prev: BattleSnapshot = cur
    var at = 0L
    var gap = 50L

    fun render(now: Long): BattleSnapshot {
        if (cur.ended) return cur
        val dt = (now - at).coerceIn(0, 150) // never extrapolate past a short stall
        val k = (dt.toFloat() / gap).coerceAtMost(1f)
        val p = prev.fighters
        return cur.copy(
            fighters = cur.fighters.mapIndexed { i, s ->
                val o = p.getOrNull(i)
                val glide = o != null && kotlin.math.abs(o.x - s.x) + kotlin.math.abs(o.y - s.y) < 0.3f
                s.copy(
                    x = if (glide) o!!.x + (s.x - o.x) * k else s.x,
                    y = if (glide) o!!.y + (s.y - o.y) * k else s.y,
                    actionAgeMs = s.actionAgeMs + dt,
                    hurtAgeMs = s.hurtAgeMs + dt,
                    projectileAgeMs = if (s.projectileAgeMs < 0) s.projectileAgeMs else s.projectileAgeMs + dt,
                )
            },
            smokes = cur.smokes.map { it.copy(ageMs = it.ageMs + dt) },
            blasts = cur.blasts.map { it.copy(ageMs = it.ageMs + dt) },
            traps = cur.traps.map { it.copy(ageMs = it.ageMs + dt) },
        )
    }
}

/** One explosion, [r] = blast radius in px, [age] ms since it went off. */
internal fun DrawScope.drawBlast(c: Offset, r: Float, age: Long) {
    val t = (age / BLAST_MS.toFloat()).coerceIn(0f, 1f)
    val grow = 1f - (1f - minOf(1f, t * 3f)).let { it * it }
    val k = 1f - t
    val fire = Color(0xFFFF6D00)
    val core = Color(0xFFFFD54F)
    // Android's RadialGradient throws on a zero radius, which is what the first frame (t = 0) gives.
    val fr = maxOf(r * grow * 1.05f, 1f)
    drawCircle(Brush.radialGradient(
        listOf(Color.White.copy(alpha = k), core.copy(alpha = k), fire.copy(alpha = 0.85f * k), Color(0xFFB71C1C).copy(alpha = 0.5f * k), Color.Transparent),
        c, fr), fr, c)
    drawCircle(core.copy(alpha = 0.8f * k), r * (0.4f + 0.75f * t), c, style = Stroke(r * 0.12f * k + 1f))
    for (i in 0 until 10) {
        val a = i * 0.6283f + 0.3f
        val d = r * (0.25f + 1.0f * t) * (if (i % 2 == 0) 1f else 0.75f)
        drawCircle((if (i % 3 == 0) core else fire).copy(alpha = k), r * 0.07f * (1f + k), c + Offset(cos(a), sin(a) * 0.8f) * d)
    }
    if (t > 0.3f) { // smoke left behind
        val sm = (t - 0.3f) / 0.7f
        drawCircle(Color(0xFF5A5A5A).copy(alpha = 0.35f * (1f - sm)), r * (0.6f + 0.3f * sm), c - Offset(0f, r * 0.3f * sm))
    }
}
