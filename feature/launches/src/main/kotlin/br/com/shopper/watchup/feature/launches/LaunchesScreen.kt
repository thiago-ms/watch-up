package br.com.shopper.watchup.feature.launches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.shopper.watchup.core.data.domain.FiltroLancamento
import br.com.shopper.watchup.core.data.domain.SecaoLancamento
import br.com.shopper.watchup.core.data.domain.combinaFiltro
import br.com.shopper.watchup.core.data.domain.deriveStatusMidia
import br.com.shopper.watchup.core.data.domain.secaoLancamento
import br.com.shopper.watchup.core.data.model.Midia
import br.com.shopper.watchup.core.data.repo.MidiaRepository
import br.com.shopper.watchup.core.ui.component.MediaPoster
import br.com.shopper.watchup.core.ui.component.SectionHeader
import br.com.shopper.watchup.core.ui.component.StatusMidiaChip
import br.com.shopper.watchup.core.ui.component.formatarData

/**
 * S006 — Lançamentos. Radar de estreias agrupado por status de data em três
 * seções fixas (Esta semana · Próximas datas · Sem data definida), com filtros por
 * tipo de data e contagem por seção. Toque abre o Detalhe; "+ Salvar" inicia o
 * cadastro pré-preenchido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaunchesScreen(
    onOpenDetail: (Long) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { MidiaRepository.get(context) }
    val todas by repo.observarTodas().collectAsStateWithLifecycle(emptyList())

    var filtro by remember { mutableStateOf(FiltroLancamento.TODOS) }

    val porSecao = remember(todas, filtro) {
        val filtradas = todas.filter { combinaFiltro(it, filtro) }
        SecaoLancamento.entries.associateWith { secao ->
            filtradas.filter { secaoLancamento(it) == secao }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Lançamentos") }) },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FiltroLancamento.entries.forEach { f ->
                    FilterChip(
                        selected = filtro == f,
                        onClick = { filtro = f },
                        label = { Text(f.rotulo) },
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SecaoLancamento.entries.forEach { secao ->
                    val itens = porSecao[secao].orEmpty()
                    item(key = "header-${secao.name}") {
                        SectionHeader(secao.titulo, trailing = "${itens.size}")
                        Spacer(Modifier.size(4.dp))
                    }
                    if (itens.isEmpty()) {
                        item(key = "empty-${secao.name}") {
                            Text(
                                "Nenhum item neste filtro.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    } else {
                        items(itens, key = { "item-${it.id}" }) { midia ->
                            LaunchRow(midia = midia, onOpen = { onOpenDetail(midia.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LaunchRow(midia: Midia, onOpen: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MediaPoster(titulo = midia.titulo, modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    midia.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatarData(midia.dataPrincipal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(4.dp))
                StatusMidiaChip(deriveStatusMidia(midia))
            }
        }
    }
}
