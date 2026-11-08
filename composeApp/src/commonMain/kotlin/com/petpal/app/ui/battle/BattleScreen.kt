package com.petpal.app.ui.battle

import androidx.compose.foundation.Canvas
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
import kotlin.math.roundToInt

@Composable
fun BattleScreen(vm: BattleViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    when (s.mode) {
        BattleScreenMode.LOBBY -> Lobby(vm, s.message)
        BattleScreenMode.HOSTING -> Waiting("Waiting for an opponent via ${s.link?.label}...", hostHint(s.link), vm::leave)
        BattleScreenMode.CONNECTING -> Waiting("Connecting to ${s.message}...", null, vm::leave)
        BattleScreenMode.SEARCHING -> Searching(s, vm)
        BattleScreenMode.FIGHT -> s.fight?.let { Fight(it, vm) }
    }
}

private fun hostHint(kind: LinkKind?) = when (kind) {
    LinkKind.HOTSPOT -> "Turn on your phone's hotspot (or join the same Wi-Fi). Your friend connects to it and taps Join."
    LinkKind.BLUETOOTH -> "Keep this screen open. Your friend taps Join under Bluetooth and picks your phone."
    null -> null
}

@Composable
private fun Lobby(vm: BattleViewModel, message: String?) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Battle", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Fight a friend nearby, no internet needed. Pets roam on their own. Time your moves for when they're in range.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LinkKind.entries.forEach { kind -> LinkCard(kind, vm) }
        NeuButton(
            onClick = vm::practice,
            icon = Icons.Rounded.SmartToy,
            text = "Practice vs CPU",
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

@Composable
private fun Fight(f: FightState, vm: BattleViewModel) {
    val me = f.snap.fighters[f.me]
    val opp = f.snap.fighters[1 - f.me]
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HpBar(f.fighters[f.me].name, me, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            Text(
                "${(f.snap.timeLeftMs / 1000)}s",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            HpBar(f.fighters[1 - f.me].name, opp, MaterialTheme.colorScheme.error, Modifier.weight(1f))
        }

        Arena(f)

        Column(Modifier.heightIn(min = 72.dp)) {
            f.log.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (f.result != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Text(f.result, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = TextAlign.Center)
            }
            NeuButton(onClick = vm::leave, text = "Back to lobby", filled = true, modifier = Modifier.fillMaxWidth())
        } else {
            val info = f.fighters[f.me]
            info.loadout.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { m ->
                        val cd = me.cooldowns.getOrElse(m.ordinal) { 0 }
                        val locked = me.has(Status.STUN)
                        NeuButton(
                            onClick = { vm.use(m) },
                            icon = m.icon(),
                            text = if (cd > 0) "${m.label} ${(cd / 100) / 10.0}s" else m.label,
                            enabled = cd == 0L && !locked,
                            accent = m.color(),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 14.dp),
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(6.dp))
                Text(
                    "${info.passive.label}: ${info.passive.description}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun pct(r: Float) = "${(r * 100).roundToInt()}"

@Composable
private fun HpBar(name: String, f: FighterSnap, color: Color, modifier: Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$name  ${f.hp}/${f.maxHp}", style = MaterialTheme.typography.labelMedium, maxLines = 1,
                modifier = Modifier.weight(1f, fill = false))
            statusIcons(f).forEach { (icon, tint) ->
                Icon(icon, null, Modifier.padding(start = 2.dp).size(14.dp), tint = tint)
            }
        }
        LinearProgressIndicator(
            progress = { f.hp.toFloat() / f.maxHp },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = if (f.hp.toFloat() / f.maxHp < 0.3f) MaterialTheme.colorScheme.error else color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun Arena(f: FightState) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().aspectRatio(ARENA_W / ARENA_H).clip(MaterialTheme.shapes.large),
    ) {
        val w = constraints.maxWidth.toFloat()
        val spriteDp = 64.dp
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.verticalGradient(listOf(HillLight, Color(0xFF5FAE62))))
            drawCircle(Color.Black.copy(alpha = 0.08f), size.width * 0.18f, center)
            f.snap.fighters.forEach { s ->
                val c = Offset(s.x * w, s.y * w)
                // ground shadow (dark, never white)
                drawOval(Color.Black.copy(alpha = 0.18f), Offset(c.x - 22.dp.toPx(), c.y + 22.dp.toPx()),
                    androidx.compose.ui.geometry.Size(44.dp.toPx(), 10.dp.toPx()))
            }
            f.snap.fighters.forEachIndexed { i, s ->
                val c = Offset(s.x * w, s.y * w)
                val info = f.fighters[i]
                val r = 34.dp.toPx()
                if (s.has(Status.GUARD)) drawCircle(ClearSky.copy(alpha = 0.7f), r, c, style = Stroke(4.dp.toPx()))
                if (s.has(Status.SLOW)) drawCircle(Color(0xFF7FC8E8).copy(alpha = 0.25f), r * 0.9f, c)
                if (s.has(Status.RAGE)) drawCircle(Color(0xFFD9534F).copy(alpha = 0.5f), r * 1.1f, c, style = Stroke(2.dp.toPx()))
                if (s.has(Status.BURN)) for (k in 0..2) {
                    drawCircle(Color(0xFFE5532D), 3.dp.toPx(), Offset(c.x - 10.dp.toPx() + k * 10.dp.toPx(), c.y - r - (k % 2) * 4.dp.toPx()))
                }
                if (s.has(Status.STUN)) for (k in 0..2) {
                    drawCircle(Sunflower, 3.dp.toPx(), Offset(c.x - 12.dp.toPx() + k * 12.dp.toPx(), c.y - r - 6.dp.toPx()))
                }
                val m = s.action ?: return@forEachIndexed
                val age = s.actionAgeMs
                val hitAt = m.windupMs
                val showing = age in (hitAt - 120)..(hitAt + 300)
                if (!showing) return@forEachIndexed
                val dir = s.facing.toFloat()
                val len = BattleStats.range(info, m) * w
                val tint = m.tint()
                when {
                    m == Move.HEAL -> drawCircle(tint.copy(alpha = 0.6f), r * 1.2f, c, style = Stroke(5.dp.toPx()))
                    m == Move.HOWL -> for (k in 1..3) drawCircle(tint.copy(alpha = 0.5f / k), r * (0.8f + k * 0.35f), c, style = Stroke(3.dp.toPx()))
                    m == Move.GUARD -> Unit
                    m == Move.FIRE_BREATH -> {
                        val path = Path().apply {
                            moveTo(c.x + dir * 20.dp.toPx(), c.y)
                            lineTo(c.x + dir * len, c.y - len * 0.22f)
                            lineTo(c.x + dir * len, c.y + len * 0.22f)
                            close()
                        }
                        drawPath(path, Brush.horizontalGradient(
                            listOf(Sunflower, tint.copy(alpha = 0.2f)).let { if (dir < 0) it.reversed() else it },
                            startX = minOf(c.x, c.x + dir * len), endX = maxOf(c.x, c.x + dir * len),
                        ))
                    }
                    m == Move.THUNDER -> {
                        val t = f.snap.fighters[1 - i].let { o -> Offset(o.x * w, o.y * w) }
                        val zig = Path().apply {
                            moveTo(t.x, 0f)
                            lineTo(t.x - 10.dp.toPx(), t.y * 0.4f); lineTo(t.x + 8.dp.toPx(), t.y * 0.6f)
                            lineTo(t.x - 6.dp.toPx(), t.y * 0.85f); lineTo(t.x, t.y)
                        }
                        drawPath(zig, tint, style = Stroke(4.dp.toPx()))
                    }
                    m.range >= 0.35f -> { // beams: water, ice, vine, gust
                        drawLine(tint.copy(alpha = 0.85f), Offset(c.x + dir * 18.dp.toPx(), c.y), Offset(c.x + dir * len, c.y),
                            strokeWidth = if (m == Move.GUST) 14.dp.toPx() else 7.dp.toPx(), cap = StrokeCap.Round)
                    }
                    else -> if (age >= hitAt) { // melee impact
                        val p = Offset(c.x + dir * 32.dp.toPx(), c.y)
                        drawCircle(tint, if (m.damage >= 10) 14.dp.toPx() else 9.dp.toPx(), p)
                    }
                }
            }
        }
        val half = with(androidx.compose.ui.platform.LocalDensity.current) { (spriteDp / 2).roundToPx() }
        f.snap.fighters.forEachIndexed { i, s ->
            val info = f.fighters[i]
            val hurt = s.hurtAgeMs < 300
            PetSprite(
                stage = info.stage,
                mood = if (s.hp < s.maxHp * 0.3f) Mood.SAD else Mood.HAPPY,
                species = info.species,
                size = spriteDp,
                modifier = Modifier
                    .offset { IntOffset((s.x * w).roundToInt() - half, (s.y * w).roundToInt() - half) }
                    .graphicsLayer {
                        scaleX = s.facing.toFloat()
                        alpha = if (hurt && (s.hurtAgeMs / 75) % 2 == 0L) 0.35f else 1f
                    },
            )
        }
    }
}

private fun statusIcons(f: FighterSnap) = buildList {
    if (f.has(Status.BURN)) add(Icons.Rounded.LocalFireDepartment to Color(0xFFE5532D))
    if (f.has(Status.SLOW)) add(Icons.Rounded.AcUnit to ClearSky)
    if (f.has(Status.STUN)) add(Icons.Rounded.Bolt to Sunflower)
    if (f.has(Status.RAGE)) add(Icons.Rounded.KeyboardDoubleArrowUp to Color(0xFFD9534F))
    if (f.has(Status.GUARD)) add(Icons.Rounded.Shield to ClearSky)
}

fun Move.icon() = when (this) {
    Move.PUNCH -> Icons.Rounded.SportsMma
    Move.CLAW -> Icons.Rounded.Pets
    Move.BITE -> Icons.Rounded.Restaurant
    Move.TACKLE -> Icons.Rounded.KeyboardDoubleArrowRight
    Move.ROCK_SLAM -> Icons.Rounded.Landscape
    Move.FIRE_BREATH -> Icons.Rounded.LocalFireDepartment
    Move.WATER_JET -> Icons.Rounded.WaterDrop
    Move.VINE_WHIP -> Icons.Rounded.Grass
    Move.ICE_SHARD -> Icons.Rounded.AcUnit
    Move.THUNDER -> Icons.Rounded.Bolt
    Move.GUST -> Icons.Rounded.Air
    Move.HEAL -> Icons.Rounded.Healing
    Move.HOWL -> Icons.Rounded.Campaign
    Move.GUARD -> Icons.Rounded.Shield
}

/** Effect color used for buttons and arena visuals. */
fun Move.tint(): Color = when (this) {
    Move.FIRE_BREATH -> Color(0xFFE5532D)
    Move.WATER_JET -> Color(0xFF3B8ED0)
    Move.ICE_SHARD -> Color(0xFF7FC8E8)
    Move.VINE_WHIP -> Color(0xFF3F9A4A)
    Move.THUNDER -> Color(0xFFE8B500)
    Move.GUST -> Color(0xFF9DB7C9)
    Move.HEAL -> Color(0xFF4CAF6A)
    Move.HOWL -> Color(0xFFD9534F)
    Move.ROCK_SLAM -> Color(0xFF8B6B4A)
    else -> Sunflower
}

@Composable
private fun Move.color() = when (this) {
    Move.PUNCH, Move.CLAW, Move.BITE, Move.TACKLE -> MaterialTheme.colorScheme.primary
    Move.GUARD -> MaterialTheme.colorScheme.secondary
    else -> tint()
}
