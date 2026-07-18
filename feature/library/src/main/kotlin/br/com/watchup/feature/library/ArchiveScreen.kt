package br.com.watchup.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.watchup.core.data.model.Midia
import br.com.watchup.core.data.repo.MidiaRepository
import br.com.watchup.core.ui.component.EmptyState
import br.com.watchup.core.ui.component.MediaPoster
import br.com.watchup.core.ui.component.PushScreenScaffold
import kotlinx.coroutines.launch

/**
 * Arquivo (item 9) — lista as mídias arquivadas, fora da biblioteca ativa. Caminho
 * de acesso discreto: Biblioteca → Configurações → "Arquivadas". Cada item abre o
 * Detalhe; o botão de desarquivar devolve a mídia à biblioteca.
 */
@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { MidiaRepository.get(context) }
    val scope = rememberCoroutineScope()
    val todas by repo.observarTodas().collectAsStateWithLifecycle(null)

    val arquivadas = remember(todas) { todas.orEmpty().filter { it.arquivada } }

    PushScreenScaffold(title = "Arquivadas", onBack = onBack) { innerPadding ->
        when {
            todas == null -> Unit // carregando
            arquivadas.isEmpty() -> EmptyState(
                mensagem = "Nada arquivado. Arquive mídias pelo detalhe para limpar a biblioteca ativa.",
                modifier = Modifier.padding(innerPadding),
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(arquivadas, key = { it.id }) { midia ->
                    ArchiveCell(
                        midia = midia,
                        onClick = { onOpenDetail(midia.id) },
                        onDesarquivar = { scope.launch { repo.salvar(midia.copy(arquivada = false)) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchiveCell(midia: Midia, onClick: () -> Unit, onDesarquivar: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Box {
            MediaPoster(
                titulo = midia.titulo,
                posterUrl = midia.posterUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f),
            )
            // Botão de desarquivar sobreposto ao pôster.
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clickable(onClick = onDesarquivar),
            ) {
                Icon(
                    Icons.Filled.Unarchive,
                    contentDescription = "Desarquivar",
                    modifier = Modifier.padding(4.dp).height(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            midia.titulo,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
