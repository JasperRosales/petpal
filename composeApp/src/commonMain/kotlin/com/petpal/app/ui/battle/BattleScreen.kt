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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
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
    }
}

private fun hostHint(kind: LinkKind?) = when (kind) {
    LinkKind.HOTSPOT -> "Turn on your phone's hotspot (or join the same Wi-Fi). Your friend connects to it and taps Join."
    LinkKind.BLUETOOTH -> "Keep this screen open. Your friend taps Join under Bluetooth and picks your phone."
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
            "Fight a friend nearby, no internet needed. Pets roam on their own. Time your moves for when they're in range. Hits charge your energy bar for Special moves; Your unique (slot 5) works twice per battle.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        FormatPicker(format, vm::setFormat)
        LinkKind.entries.forEach { kind -> LinkCard(kind, vm) }
        NeuButton(
            onClick = vm::practice,
            icon = Icons.Rounded.SmartToy,
            text = when (format) {
                BattleFormat.SOLO -> "Practice vs CPU"
                BattleFormat.TEAMS -> "3v3 vs CPUs (with CPU partners)"
                BattleFormat.FFA -> "Free-for-all vs 3 CPUs"
                else -> "Duo vs CPU (with a CPU partner)"
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Online battles: win +30 XP, draw +15, loss +10 (first 3 per day). Stronger pets come from walking.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FormatPicker(format: BattleFormat, onPick: (BattleFormat) -> Unit) {
    val duo = format == BattleFormat.DUO_RIVAL || format == BattleFormat.DUO_PARTNER
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Format", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeuButton(onClick = { onPick(BattleFormat.SOLO) }, text = "1v1", icon = Icons.Rounded.Person,
                selected = format == BattleFormat.SOLO, modifier = Modifier.weight(1f))
            NeuButton(onClick = { if (!duo) onPick(BattleFormat.DUO_RIVAL) }, text = "Duo 2v2",
                icon = Icons.Rounded.Groups, selected = duo, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeuButton(onClick = { onPick(BattleFormat.TEAMS) }, text = "Teams", icon = Icons.Rounded.Diversity3,
                selected = format == BattleFormat.TEAMS, modifier = Modifier.weight(1f))
            NeuButton(onClick = { onPick(BattleFormat.FFA) }, text = "Free-for-all", icon = Icons.Rounded.EmojiEvents,
                selected = format == BattleFormat.FFA, modifier = Modifier.weight(1f))
        }
        if (duo) {
            Text("A friend who joins plays as...", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeuButton(onClick = { onPick(BattleFormat.DUO_RIVAL) }, text = "Rival", icon = Icons.Rounded.SportsMma,
                    selected = format == BattleFormat.DUO_RIVAL, modifier = Modifier.weight(1f))
                NeuButton(onClick = { onPick(BattleFormat.DUO_PARTNER) }, text = "Partner", icon = Icons.Rounded.Handshake,
                    selected = format == BattleFormat.DUO_PARTNER, modifier = Modifier.weight(1f))
            }
        }
        if (format != BattleFormat.SOLO) Text(
            when (format) {
                BattleFormat.DUO_RIVAL -> "You + CPU vs friend + CPU. Empty slots are filled with CPU pets at your level."
                BattleFormat.DUO_PARTNER -> "You + friend vs 2 CPUs. Empty slots are filled with CPU pets at your level."
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
            if (!r.host) NeuButton(onClick = { vm.switchTeam(r.you) }, text = "Switch team", icon = Icons.Rounded.SwapHoriz,
                modifier = Modifier.fillMaxWidth())
        }
        if (r.host) NeuButton(
            onClick = vm::startRoom, enabled = r.canStart, filled = true, icon = Icons.Rounded.PlayArrow,
            text = when {
                r.canStart -> "Start (${r.players.size} players)"
                r.ffa -> "Need at least 2 players"
                else -> "Need someone on each team"
            },
            modifier = Modifier.fillMaxWidth(),
        )
        NeuButton(onClick = vm::leave, text = if (r.host) "Close room" else "Leave", icon = Icons.Rounded.Close,
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
                Icon(if (kind == LinkKind.BLUETOOTH) Icons.Rounded.Bluetooth else Icons.Rounded.WifiTethering, null)
                Spacer(Modifier.width(10.dp))
                Text(kind.label, style = MaterialTheme.typography.titleMedium)
            }
            if (!available) {
                Text("Not available on this device", style = MaterialTheme.typography.bodySmall)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NeuButton(
                        onClick = { pending = { vm.host(kind) }; ask() },
                        text = "Host", icon = Icons.Rounded.Podcasts, filled = true, surface = surface,
                        modifier = Modifier.weight(1f),
                    )
                    NeuButton(
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
        NeuButton(onClick = onCancel, text = "Cancel", icon = Icons.Rounded.Close)
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
            if (s.link == LinkKind.HOTSPOT) "Connect to your friend's hotspot first." else "Your friend should tap Host under Bluetooth.",
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
        NeuButton(onClick = vm::leave, text = "Back", icon = Icons.Rounded.Close, modifier = Modifier.fillMaxWidth())
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
    Column(Modifier.fillMaxSize().background(EmberCanvas)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Arena(f, Modifier.fillMaxSize())
            // HUD
            Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    myTeam.forEach { i ->
                        val o = f.fighters[i]
                        val tag = when { i == f.me -> "You · "; i in f.humans -> "Friend · "; else -> "" }
                        HpBar("$tag${o.name}", "Lv ${o.level}", f.snap.fighters[i],
                            if (i == f.me) MaterialTheme.colorScheme.primary else AllyBlue, Modifier.fillMaxWidth())
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 6.dp)) {
                    Text("${f.snap.timeLeftMs / 1000}s", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Icon(
                        if (soundOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                        contentDescription = if (soundOn) "Mute sounds" else "Unmute sounds",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp).clip(CircleShape)
                            .clickable { soundOn = !soundOn; sound.enabled = soundOn }.padding(4.dp),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    rivals.forEach { i ->
                        val o = f.fighters[i]
                        HpBar(if (i in f.humans) "Friend · ${o.name}" else o.name, "Lv ${o.level}", f.snap.fighters[i],
                            MaterialTheme.colorScheme.error, Modifier.fillMaxWidth())
                    }
                }
            }
            TerrainBadge(f)
            }
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
                            NeuButton(onClick = vm::battleAgain, modifier = Modifier.weight(1f), text = "Battle again",
                                icon = Icons.Rounded.Replay, filled = true, enabled = armed, contentPadding = pad,
                                surface = MaterialTheme.colorScheme.tertiaryContainer)
                            NeuButton(onClick = vm::leave, modifier = Modifier.weight(1f), text = "Back to lobby",
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
    Column(Modifier.padding(top = 4.dp)) {
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
        Spacer(Modifier.width(10.dp))
        Text("Unique", style = MaterialTheme.typography.labelSmall)
        repeat(BattleRules.UNIQUE_USES) { k ->
            Box(
                Modifier.padding(start = 4.dp).size(10.dp).clip(CircleShape)
                    .background(if (k < me.uniqueLeft) MoveCategory.UNIQUE.color() else MaterialTheme.colorScheme.surfaceVariant),
            )
        }
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
    val needsEnergy = (m.category == MoveCategory.SPECIAL && me.energy < BattleRules.MAX_ENERGY) || me.energy < m.energyCost
    val usedUp = m.category == MoveCategory.UNIQUE && me.uniqueLeft <= 0
    val enabled = cd == 0L && !needsEnergy && !usedUp && !me.has(Status.STUN)
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
                    usedUp -> "used up"
                    cd > 0 -> "${(cd / 100) / 10.0}s"
                    needsEnergy -> "need energy"
                    m.category == MoveCategory.UNIQUE -> "${me.uniqueLeft} left"
                    m.energyCost > 0 -> "⚡${m.energyCost}"
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
private fun HpBar(name: String, level: String, f: FighterSnap, color: Color, modifier: Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$name  $level", style = MaterialTheme.typography.labelMedium, maxLines = 1, color = Color.White,
                modifier = Modifier.weight(1f, fill = false))
            statusIcons(f).forEach { (icon, tint) ->
                Icon(icon, null, Modifier.padding(start = 2.dp).size(14.dp), tint = tint)
            }
        }
        LinearProgressIndicator(
            progress = { f.hp.toFloat() / f.maxHp },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = if (f.hp.toFloat() / f.maxHp < 0.3f) MaterialTheme.colorScheme.error else color,
            trackColor = Color.White.copy(alpha = 0.3f),
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Text("${f.hp}/${f.maxHp}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
    }
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
private fun DrawScope.drawImpact(c: Offset, t: Float) {
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
private fun DrawScope.drawMove(s: FighterSnap, info: FighterInfo, sx: Float, sy: Float) {
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
            drawLine(Brush.linearGradient(listOf(Color.Transparent, tint.copy(alpha = 0.8f)), from, p(bx, by)),
                from, p(bx, by), hs * 1.4f * sx, StrokeCap.Round)
            val glow = circle(bx, by, hs * 1.6f)
            drawOval(Brush.radialGradient(listOf(Color.White, m.coreTint(), tint, tint.copy(alpha = 0f)), p(bx, by), hs * 1.6f * sx),
                glow.topLeft, glow.size)
        }
        HitShape.MELEE -> {
            if (after > 260) return
            val t = after / 260f
            val k = 1f - t
            val slashes = if (m.effect == Effect.MULTI) 3 else if (big) 2 else 1
            for (n in 0 until slashes) {
                val shift = (n - (slashes - 1) / 2f) * hs * 0.45f
                val r = hs * (0.7f + 0.3f * t)
                val path = Path().apply {
                    moveTo(p(cx + px * r + dx * shift, cy + py * r + dy * shift))
                    quadTo(p(cx + dx * (r * 0.9f + shift), cy + dy * (r * 0.9f + shift)),
                        p(cx - px * r + dx * shift, cy - py * r + dy * shift))
                }
                drawPath(path, tint.copy(alpha = 0.55f * k), style = Stroke((if (big) 12 else 8).dp.toPx(), cap = StrokeCap.Round))
                drawPath(path, Color.White.copy(alpha = k), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        HitShape.BEAM -> {
            if (after > 380) return
            val k = 1f - after / 380f
            val grow = minOf(1f, after / 90f)
            drawPath(beamPath(range * grow, hs), tint.copy(alpha = 0.55f * k))
            drawPath(beamPath(range * grow, hs * 0.45f), m.coreTint().copy(alpha = 0.9f * k))
            drawPath(beamPath(range * grow, hs * 0.15f), Color.White.copy(alpha = k))
            val ring = circle(ox, oy, hs * 1.3f)
            drawOval(Color.White.copy(alpha = 0.7f * k), ring.topLeft, ring.size)
        }
        HitShape.CONE -> {
            if (after > 420) return
            val t = after / 420f
            val k = 1f - t
            val reach = range * minOf(1f, 0.35f + t * 1.6f)
            drawPath(conePath(reach), Brush.radialGradient(
                listOf(m.coreTint().copy(alpha = 0.95f * k), tint.copy(alpha = 0.75f * k), tint.copy(alpha = 0.1f * k)),
                p(ox, oy), (reach * sx).coerceAtLeast(1f),
            ))
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
            val zig = Path().apply {
                moveTo(tgt.x + r, 0f)
                var y = 0f
                var left = true
                while (y < tgt.y) {
                    y = minOf(tgt.y, y + tgt.y / 6 + 1f)
                    left = !left
                    lineTo(tgt.x + if (y >= tgt.y) 0f else if (left) -r * 1.6f else r * 1.6f, y)
                }
            }
            val join = StrokeJoin.Round
            drawPath(zig, tint.copy(alpha = 0.6f * k), style = Stroke((if (big) 10 else 6).dp.toPx(), cap = StrokeCap.Round, join = join))
            drawPath(zig, Color.White.copy(alpha = k), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = join))
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
private fun Move.coreTint(): Color = when (element) {
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
    val oppColor = Color(0xFFE74C3C)
    BoxWithConstraints(modifier.clipToBounds()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val sx = w / ARENA_W
        val sy = h / ARENA_H
        fun pos(s: FighterSnap) = Offset(s.x * sx, s.y * sy)
        val spriteDp = 72.dp
        Canvas(Modifier.matchParentSize()) {
            if (f.terrain == Terrain.ARENA) drawArenaFloor() else drawTerrainFloor(f.terrain)
            f.snap.traps.forEach { tr -> drawTrap(tr, Offset(tr.x * sx, tr.y * sy), Trap.RADIUS * minOf(sx, sy)) }
            f.snap.fighters.forEachIndexed { i, s ->
                val c = pos(s)
                if (s.hp <= 0) return@forEachIndexed
                val ring = when { i == f.me -> meColor; teamOf(f, i) == teamOf(f, f.me) -> AllyBlue; else -> oppColor }
                drawOval(Color.Black.copy(alpha = 0.2f), Offset(c.x - 26.dp.toPx(), c.y + 24.dp.toPx()),
                    Size(52.dp.toPx(), 12.dp.toPx()))
                // hurtbox: exactly the circle moves are tested against (yours orange, theirs red)
                val hr = Size(PET_RADIUS * sx * 2, PET_RADIUS * sy * 2)
                val tl = Offset(c.x - hr.width / 2, c.y - hr.height / 2)
                if (!s.has(Status.VANISH)) {
                    drawOval(ring.copy(alpha = 0.10f), tl, hr)
                    drawOval(ring.copy(alpha = 0.55f), tl, hr, style = Stroke(1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
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
private fun DrawScope.drawTrap(t: TrapSnap, c: Offset, r: Float) {
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
            traps = cur.traps.map { it.copy(ageMs = it.ageMs + dt) },
        )
    }
}
