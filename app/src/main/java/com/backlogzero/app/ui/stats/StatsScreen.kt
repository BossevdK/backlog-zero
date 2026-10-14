package com.backlogzero.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.backlogzero.app.data.LibraryRepository
import com.backlogzero.app.domain.ItemStatus
import com.backlogzero.app.domain.LibraryStats
import com.backlogzero.app.domain.MediaType
import com.backlogzero.app.domain.StatsCalculator
import com.backlogzero.app.ui.EmptyState
import com.backlogzero.app.ui.LoadGate
import com.backlogzero.app.ui.SectionTitle
import com.backlogzero.app.ui.appViewModel
import com.backlogzero.app.ui.icon
import com.backlogzero.app.ui.loader
import com.backlogzero.app.ui.theme.color
import kotlinx.coroutines.flow.map
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

class StatsViewModel(library: LibraryRepository) : ViewModel() {
    private val data = loader { library.observeItems().map { StatsCalculator.calculate(it, System.currentTimeMillis()) } }
    val state get() = data.state
    fun retry() = data.retry()
}

@Composable
fun StatsScreen() {
    val vm = appViewModel { StatsViewModel(it.library) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Statistieken", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp))
        LoadGate(state, vm::retry) { stats ->
            if (stats.total == 0) {
                EmptyState(Icons.Filled.BarChart, "Nog niets om te tonen", "Zodra je items toevoegt, zie je hier je voortgang.")
            } else {
                StatsContent(stats)
            }
        }
    }
}

@Composable
private fun StatsContent(stats: LibraryStats) {
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tile("Totaal", stats.total.toString(), "items", Modifier.weight(1f))
                    Tile("Afgerond", "${(stats.completionRate * 100).roundToInt()}%", "van je bibliotheek", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tile("Dit jaar afgerond", stats.completedThisYear.toString(), "items", Modifier.weight(1f))
                    Tile(
                        "Gemiddeld cijfer",
                        stats.averageRating?.let { String.format(Locale("nl"), "%.1f", it) } ?: "–",
                        if (stats.ratedCount == 0) "nog niets beoordeeld" else "van ${stats.ratedCount} beoordeeld",
                        Modifier.weight(1f),
                    )
                }
            }
        }
        item { StatusDistribution(stats) }
        item { ByType(stats) }
        item { PerMonth(stats) }
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)) { content() }
}

@Composable
private fun Tile(label: String, value: String, caption: String, modifier: Modifier) {
    Panel(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineLarge)
        Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusDistribution(stats: LibraryStats) {
    Panel {
        SectionTitle("Verdeling per status")
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(50))) {
            ItemStatus.entries.forEach { s ->
                val count = stats.byStatus[s] ?: 0
                if (count > 0) Box(Modifier.weight(count.toFloat()).fillMaxHeight().background(s.color()))
            }
        }
        Spacer(Modifier.height(14.dp))
        ItemStatus.entries.forEach { s ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(s.color()))
                Spacer(Modifier.width(10.dp))
                Text(s.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text((stats.byStatus[s] ?: 0).toString(), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun ByType(stats: LibraryStats) {
    Panel {
        SectionTitle("Per categorie")
        Spacer(Modifier.height(8.dp))
        MediaType.entries.forEach { type ->
            val t = stats.byType[type] ?: return@forEach
            Column(Modifier.padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(type.icon(), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(type.plural, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("${t.completed} / ${t.total} afgerond", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (t.total == 0) 0f else t.completed.toFloat() / t.total },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }
    }
}

@Composable
private fun PerMonth(stats: LibraryStats) {
    val max = (stats.completedPerMonth.maxOfOrNull { it.completed } ?: 0).coerceAtLeast(1)
    Panel {
        SectionTitle("Afgerond per maand")
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            stats.completedPerMonth.forEach { m ->
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text(m.completed.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    val fraction = m.completed.toFloat() / max
                    Box(
                        Modifier.fillMaxWidth().height((88 * fraction).coerceAtLeast(3f).dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (m.completed > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        m.month.month.getDisplayName(TextStyle.SHORT, Locale("nl")).trimEnd('.'),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
