package br.com.watchup.feature.library

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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import br.com.watchup.core.data.domain.FiltroBiblioteca
import br.com.watchup.core.data.domain.combinaFiltroBiblioteca
import br.com.watchup.core.data.domain.novosEpisodiosEstimados
import br.com.watchup.core.data.model.Midia
import br.com.watchup.core.data.model.StatusUsuario
import br.com.watchup.core.data.repo.MidiaRepository
import br.com.watchup.core.ui.component.EmptyState
import br.com.watchup.core.ui.component.MediaPoster
import br.com.watchup.core.ui.component.StatusUsuarioChip

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
    onAbrirConfig: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { MidiaRepository.get(context) }
    // null = ainda carregando (evita piscar o estado vazio antes da 1ª emissão do Room).
    val todas by repo.observarTodas().collectAsStateWithLifecycle(null)

    var filtro by remember { mutableStateOf(FiltroBiblioteca.TODOS) }
    var busca by remember { mutableStateOf("") }
    // Item 10: por padrão a biblioteca esconde as mídias vistas; o chip "Vistos" as revela.
    var mostrarVistos by remember { mutableStateOf(false) }
    // Item 11: filtro para mostrar só os favoritos.
    var soFavoritos by remember { mutableStateOf(false) }
    // Item 8: modo que mostra só as "intenções de assistir" (ocultas por padrão).
    var soIntencoes by remember { mutableStateOf(false) }
    val visiveis = remember(todas, filtro, busca, mostrarVistos, soFavoritos, soIntencoes) {
        todas.orEmpty().filter {
            if (it.arquivada) return@filter false // item 9: arquivadas fora da biblioteca ativa
            if (!combinaFiltroBiblioteca(it, filtro)) return@filter false
            if (busca.isNotBlank() && !it.titulo.contains(busca.trim(), ignoreCase = true)) return@filter false
            if (soIntencoes) {
                it.intencao // item 8: modo "intenções" mostra só rascunhos
            } else {
                !it.intencao &&
                    (mostrarVistos || it.statusUsuario != StatusUsuario.VISTO) &&
                    (!soFavoritos || it.favorito)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biblioteca") },
                actions = {
                    IconButton(onClick = onAbrirConfig) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configurações")
                    }
                },
            )
        },
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
                // Toggles independentes dos filtros por tipo.
                FilterChip(
                    selected = soFavoritos,
                    onClick = { soFavoritos = !soFavoritos },
                    label = { Text("Favoritos") },
                    leadingIcon = {
                        Icon(
                            if (soFavoritos) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.height(18.dp),
                        )
                    },
                )
                FilterChip(
                    selected = mostrarVistos,
                    onClick = { mostrarVistos = !mostrarVistos },
                    label = { Text("Vistos") },
                    leadingIcon = if (mostrarVistos) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.height(18.dp)) }
                    } else {
                        null
                    },
                )
                FilterChip(
                    selected = soIntencoes,
                    onClick = { soIntencoes = !soIntencoes },
                    label = { Text("Intenções") },
                    leadingIcon = {
                        Icon(
                            if (soIntencoes) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.height(18.dp),
                        )
                    },
                )
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
                posterUrl = midia.posterUrl,
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
            if (midia.favorito) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Favorito",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .height(18.dp),
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
        val novos = novosEpisodiosEstimados(midia)
        Text(
            if (novos > 0) "${midia.tipo.rotulo} · +$novos ep." else midia.tipo.rotulo,
            style = MaterialTheme.typography.labelSmall,
            color = if (novos > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
