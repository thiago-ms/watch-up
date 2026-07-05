package br.com.shopper.watchup.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import br.com.shopper.watchup.core.data.domain.FiltroBiblioteca
import br.com.shopper.watchup.core.data.domain.combinaFiltroBiblioteca
import br.com.shopper.watchup.core.data.model.Midia
import br.com.shopper.watchup.core.data.repo.MidiaRepository
import br.com.shopper.watchup.core.ui.component.EmptyState
import br.com.shopper.watchup.core.ui.component.MediaPoster
import br.com.shopper.watchup.core.ui.component.StatusUsuarioChip

/**
 * S00x — Biblioteca. Acervo pessoal em grade de 3 colunas, com filtros por tipo
 * ("Séries" agrupa série/programa/reality). Chip do status do usuário sobreposto
 * ao pôster, título e tipo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenDetail: (Long) -> Unit,
    onAdicionar: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { MidiaRepository.get(context) }
    // null = ainda carregando (evita piscar o estado vazio antes da 1ª emissão do Room).
    val todas by repo.observarTodas().collectAsStateWithLifecycle(null)

    var filtro by remember { mutableStateOf(FiltroBiblioteca.TODOS) }
    var busca by remember { mutableStateOf("") }
    val visiveis = remember(todas, filtro, busca) {
        todas.orEmpty().filter {
            combinaFiltroBiblioteca(it, filtro) &&
                (busca.isBlank() || it.titulo.contains(busca.trim(), ignoreCase = true))
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Biblioteca") }) },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                label = { Text("Filtrar por título") },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FiltroBiblioteca.entries.forEach { f ->
                    FilterChip(
                        selected = filtro == f,
                        onClick = { filtro = f },
                        label = { Text(f.rotulo) },
                    )
                }
            }

            if (todas == null) {
                // Carregando: nada a exibir ainda (sem flash do estado vazio).
            } else if (visiveis.isEmpty()) {
                EmptyState(
                    mensagem = "Nada por aqui neste filtro.",
                    cta = {
                        Button(onClick = onAdicionar) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Adicionar mídia")
                        }
                    },
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visiveis, key = { it.id }) { midia ->
                        LibraryCell(midia = midia, onClick = { onOpenDetail(midia.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryCell(midia: Midia, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Box {
            MediaPoster(
                titulo = midia.titulo,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f),
            )
            StatusUsuarioChip(
                midia = midia,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            midia.titulo,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            midia.tipo.rotulo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
