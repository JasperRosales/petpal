package com.petpal.app.ui.battle

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
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
            "Fight a friend nearby, no internet needed. Pets roam on their own. Time your moves for when they're in range. Hits charge your energy bar for Special moves; Your unique (slot 5) works twice per battle.",
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

/** Set by the fight screen so the app hides its bottom bar and the arena gets the whole screen. */
val LocalBattleFullscreen = staticCompositionLocalOf<MutableState<Boolean>?> { null }

@Composable
private fun Fight(f: FightState, vm: BattleViewModel) {
    val fullscreen = LocalBattleFullscreen.current
    DisposableEffect(fullscreen) {
        fullscreen?.value = true
        onDispose { fullscreen?.value = false }
    }
    val me = f.snap.fighters[f.me]
    val opp = f.snap.fighters[1 - f.me]
    val info = f.fighters[f.me]
    Column(Modifier.fillMaxSize().background(Color(0xFF2E5E33))) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Arena(f, Modifier.fillMaxSize())
            // HUD
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HpBar("You · ${info.name}", "Lv ${info.level}", me, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                Text(
                    "${f.snap.timeLeftMs / 1000}s",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
                val o = f.fighters[1 - f.me]
                HpBar(o.name, "Lv ${o.level}", opp, MaterialTheme.colorScheme.error, Modifier.weight(1f))
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
                    Modifier.align(Alignment.Center).padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Text(f.result, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(20.dp))
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (f.result != null) {
                NeuButton(onClick = vm::leave, text = "Back to lobby", filled = true, modifier = Modifier.fillMaxWidth())
            } else {
                EnergyRow(me)
                info.loadout.withIndex().chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (i, m) ->
                            MoveButton(m, me.cooldowns.getOrElse(i) { 0 }, info, me, Modifier.weight(1f)) { vm.use(m) }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnergyRow(me: FighterSnap) {
    val full = me.energy >= BattleRules.MAX_ENERGY
    val energyColor = if (full) Sunflower else ClearSky
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
private fun MoveButton(m: Move, cd: Long, info: FighterInfo, me: FighterSnap, modifier: Modifier, onClick: () -> Unit) {
    val needsEnergy = m.category == MoveCategory.SPECIAL && me.energy < BattleRules.MAX_ENERGY
    val usedUp = m.category == MoveCategory.UNIQUE && me.uniqueLeft <= 0
    val enabled = cd == 0L && !needsEnergy && !usedUp && !me.has(Status.STUN)
    val accent = m.tint()
    val total = BattleStats.cooldown(info, m).coerceAtLeast(1)
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

/** Fixed (seeded) grass decorations in arena units, so both phones and every frame look the same. */
private class GrassDecor(val tufts: List<Offset>, val flowers: List<Pair<Offset, Color>>, val patches: List<Pair<Offset, Float>>)

private val grassDecor by lazy {
    val r = kotlin.random.Random(42)
    val flowerColors = listOf(Color.White, Color(0xFFFFE066), Color(0xFFF7A1C4), Color(0xFFB4A7F5))
    GrassDecor(
        tufts = List(140) { Offset(r.nextFloat(), r.nextFloat()) },
        flowers = List(28) { Offset(r.nextFloat(), r.nextFloat()) to flowerColors[r.nextInt(flowerColors.size)] },
        patches = List(10) { Offset(r.nextFloat(), r.nextFloat()) to (0.08f + r.nextFloat() * 0.1f) },
    )
}

private val GrassLight = Color(0xFF7FCB6E)
private val GrassMid = Color(0xFF5DAE4F)
private val GrassDark = Color(0xFF3E8A3A)
private val GrassBlade = Color(0xFF2F6E2C)

private fun DrawScope.drawGrassField() {
    drawRect(Brush.verticalGradient(listOf(GrassLight, GrassMid)))
    // mowed stripes
    val stripes = 8
    val sh = size.height / stripes
    for (k in 0 until stripes step 2) drawRect(Color.White.copy(alpha = 0.05f), Offset(0f, k * sh), Size(size.width, sh))
    grassDecor.patches.forEach { (p, rad) ->
        drawOval(GrassDark.copy(alpha = 0.18f), Offset(p.x * size.width - rad * size.width, p.y * size.height - rad * size.width * 0.5f),
            Size(rad * size.width * 2, rad * size.width))
    }
    val blade = 7.dp.toPx()
    val stroke = 1.6.dp.toPx()
    grassDecor.tufts.forEach { t ->
        val b = Offset(t.x * size.width, t.y * size.height)
        drawLine(GrassBlade, b, Offset(b.x - blade * 0.5f, b.y - blade), stroke, StrokeCap.Round)
        drawLine(GrassBlade, b, Offset(b.x, b.y - blade * 1.3f), stroke, StrokeCap.Round)
        drawLine(GrassBlade, b, Offset(b.x + blade * 0.5f, b.y - blade), stroke, StrokeCap.Round)
    }
    val petal = 2.5.dp.toPx()
    grassDecor.flowers.forEach { (p, c) ->
        val o = Offset(p.x * size.width, p.y * size.height)
        for (a in 0 until 4) {
            val dx = if (a % 2 == 0) petal else 0f
            val dy = if (a % 2 == 1) petal else 0f
            drawCircle(c, petal, Offset(o.x + if (a < 2) dx else -dx, o.y + if (a < 2) dy else -dy))
        }
        drawCircle(Sunflower, petal * 0.8f, o)
    }
    // soft dark edges frame the field
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)),
        center, radius = size.maxDimension * 0.75f))
}

@Composable
private fun Arena(f: FightState, modifier: Modifier) {
    val bob by rememberInfiniteTransition().animateFloat(
        0f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse),
    )
    val meColor = MaterialTheme.colorScheme.primary
    val oppColor = MaterialTheme.colorScheme.error
    BoxWithConstraints(modifier.clipToBounds()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val sx = w / ARENA_W
        val sy = h / ARENA_H
        fun pos(s: FighterSnap) = Offset(s.x * sx, s.y * sy)
        val spriteDp = 72.dp
        Canvas(Modifier.matchParentSize()) {
            drawGrassField()
            f.snap.fighters.forEachIndexed { i, s ->
                val c = pos(s)
                val ring = if (i == f.me) meColor else oppColor
                drawOval(Color.Black.copy(alpha = 0.2f), Offset(c.x - 26.dp.toPx(), c.y + 24.dp.toPx()),
                    Size(52.dp.toPx(), 12.dp.toPx()))
                // team ring at the feet: yours is your theme color, theirs is red
                drawOval(ring, Offset(c.x - 30.dp.toPx(), c.y + 22.dp.toPx()), Size(60.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(3.dp.toPx()))
            }
            f.snap.fighters.forEachIndexed { i, s ->
                val c = pos(s)
                val info = f.fighters[i]
                val r = 38.dp.toPx()
                if (s.has(Status.VANISH)) { // shadow on the ground where it will reappear from
                    drawOval(Color.Black.copy(alpha = 0.25f), Offset(c.x - r * 0.6f, c.y + r * 0.4f), Size(r * 1.2f, r * 0.4f))
                }
                if (s.has(Status.COUNTER)) drawCircle(Color.White.copy(alpha = 0.8f), r * 1.15f, c, style = Stroke(3.dp.toPx()))
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
                val m = s.action ?: return@forEachIndexed
                val age = s.actionAgeMs
                val hitAt = m.windupMs
                if (age !in (hitAt - 120)..(hitAt + 300)) return@forEachIndexed
                val dir = s.facing.toFloat()
                val len = BattleStats.range(info, m) * sx
                val tint = m.tint()
                when {
                    m.effect == Effect.GUARD -> Unit
                    m.isBuff -> for (k in 1..3) drawCircle(tint.copy(alpha = 0.6f / k), r * (0.8f + k * 0.35f), c, style = Stroke(3.dp.toPx()))
                    m.element == Element.ELECTRIC && m.range >= 0.5f -> {
                        val t = pos(f.snap.fighters[1 - i])
                        val zig = Path().apply {
                            moveTo(t.x, 0f)
                            lineTo(t.x - 10.dp.toPx(), t.y * 0.4f); lineTo(t.x + 8.dp.toPx(), t.y * 0.6f)
                            lineTo(t.x - 6.dp.toPx(), t.y * 0.85f); lineTo(t.x, t.y)
                        }
                        drawPath(zig, tint, style = Stroke(if (m.category == MoveCategory.UNIQUE) 7.dp.toPx() else 4.dp.toPx()))
                    }
                    m.isFire && m.range >= 0.35f -> {
                        val spread = if (m.category == MoveCategory.UNIQUE) 0.35f else 0.22f
                        val path = Path().apply {
                            moveTo(c.x + dir * 20.dp.toPx(), c.y)
                            lineTo(c.x + dir * len, c.y - len * spread)
                            lineTo(c.x + dir * len, c.y + len * spread)
                            close()
                        }
                        drawPath(path, Brush.horizontalGradient(
                            listOf(Sunflower, tint.copy(alpha = 0.2f)).let { if (dir < 0) it.reversed() else it },
                            startX = minOf(c.x, c.x + dir * len), endX = maxOf(c.x, c.x + dir * len),
                        ))
                    }
                    m.range >= 0.35f -> {
                        val width = when {
                            m.category == MoveCategory.UNIQUE -> 18.dp
                            m.category == MoveCategory.SPECIAL -> 12.dp
                            else -> 7.dp
                        }
                        drawLine(tint.copy(alpha = 0.85f), Offset(c.x + dir * 18.dp.toPx(), c.y), Offset(c.x + dir * len, c.y),
                            strokeWidth = width.toPx(), cap = StrokeCap.Round)
                    }
                    else -> if (age >= hitAt) {
                        val p = Offset(c.x + dir * 34.dp.toPx(), c.y)
                        drawCircle(tint, (if (m.damage >= 20) 20.dp else if (m.damage >= 10) 14.dp else 9.dp).toPx(), p)
                    }
                }
            }
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
                modifier = Modifier
                    .offset { IntOffset(c.x.roundToInt() - half, c.y.roundToInt() - half) }
                    .graphicsLayer {
                        scaleX = s.facing.toFloat()
                        alpha = when {
                            s.has(Status.VANISH) -> 0f // flying high or underground
                            hurt && (s.hurtAgeMs / 75) % 2 == 0L -> 0.35f
                            else -> 1f
                        }
                    },
            )
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
    if (f.has(Status.COUNTER)) add(Icons.Rounded.Replay to Color.White)
}

fun Move.icon(): ImageVector = when (this) {
    Move.SOLAR_BEAM, Move.SOLAR_FLARE -> Icons.Rounded.WbSunny
    Move.SHADOW_CLAW, Move.NIGHT_TERROR -> Icons.Rounded.DarkMode
    Move.INFERNO, Move.DRAGON_FURY -> Icons.Rounded.Fireplace
    Move.MOON_KICK, Move.LUNAR_STRIKE -> Icons.Rounded.Brightness3
    Move.PACK_CALL, Move.ALPHA_HOWL -> Icons.Rounded.Groups
    Move.STORM_EYE, Move.TEMPEST_EYE -> Icons.Rounded.Visibility
    Move.FORTRESS, Move.CITADEL -> Icons.Rounded.Security
    Move.FOXFIRE, Move.NINE_TAILS -> Icons.Rounded.AutoAwesome
    Move.TIDAL_WAVE, Move.TSUNAMI -> Icons.Rounded.Waves
    Move.MAUL, Move.GRIZZLY_RAMPAGE -> Icons.Rounded.FrontHand
    else -> when (effect) {
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
        }
    }
}

/** Effect color used for buttons and arena visuals. */
fun Move.tint(): Color = when {
    this == Move.SHADOW_CLAW || this == Move.NIGHT_TERROR || this == Move.MOON_KICK || this == Move.LUNAR_STRIKE ->
        Color(0xFF7E57C2)
    this == Move.FOXFIRE || this == Move.NINE_TAILS -> Color(0xFF6C8CFF)
    effect == Effect.GUARD -> ClearSky
    effect in setOf(Effect.HEAL, Effect.PACK_CALL, Effect.ALPHA) -> Color(0xFF4CAF6A)
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
}

@Composable
fun MoveCategory.color(): Color = when (this) {
    MoveCategory.NORMAL -> MaterialTheme.colorScheme.outline
    MoveCategory.SPECIAL -> ClearSky
    MoveCategory.UNIQUE -> Color(0xFF9C27B0)
}
