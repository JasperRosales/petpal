package com.petpal.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.petpal.app.domain.battle.Element
import com.petpal.app.domain.battle.Move
import com.petpal.app.domain.battle.MoveCategory
import com.petpal.app.domain.battle.SpeciesKit
import com.petpal.app.domain.model.Pet
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import com.petpal.app.ui.battle.MovePreviewDialog
import com.petpal.app.ui.battle.color
import com.petpal.app.ui.battle.icon
import com.petpal.app.ui.battle.tint

/** One move with icon, stats and a trailing label. */
@Composable
fun MoveRow(
    move: Move,
    trailing: String,
    modifier: Modifier = Modifier,
    onView: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        leading?.invoke()
        Icon(move.icon(), null, Modifier.size(20.dp), tint = move.tint())
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(move.label, style = MaterialTheme.typography.bodyMedium)
            Text(move.describe(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(trailing, style = MaterialTheme.typography.labelSmall, color = move.category.color())
        if (onView != null) IconButton(onClick = onView, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.PlayCircle, contentDescription = "View ${move.label}", tint = move.tint())
        }
    }
}

/** Shown when a new move unlocks but all [SpeciesKit.MAX_MOVES] slots are taken. Must be answered. */
@Composable
fun LearnMoveDialog(pet: Pet, move: Move, onDecide: (forget: Move?) -> Unit) {
    var forget by remember(move) { mutableStateOf<Move?>(null) }
    var preview by remember { mutableStateOf<Move?>(null) }
    preview?.let { MovePreviewDialog(pet, it) { preview = null } }
    AlertDialog(
        onDismissRequest = {},
        title = { Text("${pet.name} wants to learn ${move.label}!") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MoveRow(move, move.category.label, onView = { preview = move })
                Text("But it already knows ${SpeciesKit.MAX_MOVES} moves. Forget one?",
                    style = MaterialTheme.typography.bodyMedium)
                pet.moves.forEach { m ->
                    MoveRow(
                        m, m.category.label,
                        Modifier.fillMaxWidth().clickable { forget = m },
                        leading = { RadioButton(selected = forget == m, onClick = { forget = m }) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDecide(forget) }, enabled = forget != null) { Text("Forget & learn") }
        },
        dismissButton = { TextButton(onClick = { onDecide(null) }) { Text("Don't learn") } },
    )
}

/** Every move in the game, marking which ones this pet can learn and when. */
@Composable
fun MoveListDialog(pet: Pet, onDismiss: () -> Unit) {
    val (babyUnique, kidUnique, adultUnique) = SpeciesKit.uniques(pet.species)
    val types = SpeciesKit.affinity(pet.species)
    var preview by remember { mutableStateOf<Move?>(null) }
    preview?.let { MovePreviewDialog(pet, it) { preview = null } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().padding(12.dp), shape = MaterialTheme.shapes.large) {
            Column {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("All moves", style = MaterialTheme.typography.titleLarge)
                        Text("${pet.species.label} types: Normal, ${types.joinToString { it.label }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Element.entries.forEach { el ->
                        val moves = Move.entries.filter { it.element == el && it.category != MoveCategory.UNIQUE }
                        item(key = el.name) {
                            Text(el.label, style = MaterialTheme.typography.titleSmall, color = el.tint(),
                                modifier = Modifier.padding(top = 8.dp))
                        }
                        items(moves, key = { it.name }) { m ->
                            val can = SpeciesKit.canLearn(pet.species, m)
                            val label = when {
                                m in pet.moves -> "Known"
                                !can -> "Can't learn"
                                m.element != Element.NORMAL && m.elements.none { it in types } -> "Lv ${m.level} ★"
                                else -> "Lv ${m.level}"
                            }
                            MoveRow(m, label, Modifier.alpha(if (can) 1f else 0.4f), onView = { preview = m })
                        }
                    }
                    item { Text("Uniques", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
                    items(Move.entries.filter { it.category == MoveCategory.UNIQUE }, key = { it.name }) { m ->
                        val label = when (m) {
                            babyUnique -> "Baby"
                            kidUnique -> "Kid"
                            adultUnique -> "Adult"
                            else -> "Can't learn"
                        }
                        MoveRow(m, label, Modifier.alpha(if (m == babyUnique || m == kidUnique || m == adultUnique) 1f else 0.4f),
                            onView = { preview = m })
                    }
                    item {
                        Text("★ = off-type move this species can still learn.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
