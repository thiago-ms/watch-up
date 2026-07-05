package br.com.watchup.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.watchup.core.ui.component.MediaPoster
import kotlinx.coroutines.launch

/**
 * S008 — Buscar. Consulta o catálogo do TMDB (`/search/multi`) e lista título,
 * ano, tipo e pôster. Tocar num resultado abre o cadastro já pré-preenchido
 * (título, ano, tipo e a URL do pôster).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onSelecionar: (titulo: String, ano: String?, tipoNome: String, generos: List<String>, posterUrl: String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var termo by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }
    var buscou by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var resultados by remember { mutableStateOf<List<TmdbResultado>>(emptyList()) }

    var temChave by remember { mutableStateOf(TmdbConfig.temChave(context)) }
    var mostrarDialogoChave by remember { mutableStateOf(false) }

    fun buscar() {
        if (termo.isBlank()) return
        if (!temChave) { mostrarDialogoChave = true; return }
        scope.launch {
            carregando = true
            erro = null
            buscou = true
            try {
                resultados = TmdbClient.buscar(TmdbConfig.getApiKey(context), termo)
            } catch (e: TmdbException) {
                erro = e.message
                resultados = emptyList()
            } finally {
                carregando = false
            }
        }
    }

    if (mostrarDialogoChave) {
        DialogoChave(
            chaveAtual = TmdbConfig.getApiKey(context),
            onSalvar = { nova ->
                TmdbConfig.setApiKey(context, nova)
                temChave = TmdbConfig.temChave(context)
                mostrarDialogoChave = false
            },
            onCancelar = { mostrarDialogoChave = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar") },
                actions = {
                    IconButton(onClick = { mostrarDialogoChave = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Chave do TMDB")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            OutlinedTextField(
                value = termo,
                onValueChange = { termo = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = {
                    IconButton(onClick = { buscar() }) {
                        Icon(Icons.Filled.Search, contentDescription = "Buscar")
                    }
                },
                label = { Text("Buscar filme ou série") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { buscar() }),
            )

            when {
                carregando -> CentroPreenchido { CircularProgressIndicator() }

                erro != null -> Aviso(
                    icone = Icons.Filled.TravelExplore,
                    titulo = "Não deu certo",
                    detalhe = erro!!,
                )

                buscou && resultados.isEmpty() -> Aviso(
                    icone = Icons.Filled.Search,
                    titulo = "Nada encontrado",
                    detalhe = "Tente outro termo.",
                )

                !temChave -> Aviso(
                    icone = Icons.Filled.Settings,
                    titulo = "Informe a chave do TMDB",
                    detalhe = "Toque na engrenagem (↗) e cole sua API Key do TMDB para buscar no catálogo.",
                )

                !buscou -> Aviso(
                    icone = Icons.Filled.TravelExplore,
                    titulo = "Busque no catálogo",
                    detalhe = "Digite o nome de um filme ou série para ver resultados do TMDB.",
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(resultados) { r ->
                        ResultadoRow(r, onClick = {
                            onSelecionar(r.titulo, r.ano, r.tipo.name, r.generos, r.posterUrl)
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoChave(
    chaveAtual: String,
    onSalvar: (String) -> Unit,
    onCancelar: () -> Unit,
) {
    var texto by remember { mutableStateOf(chaveAtual) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Chave do TMDB") },
        text = {
            Column {
                Text(
                    "Cole sua API Key (v3 auth) do themoviedb.org. Fica guardada só neste aparelho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("API Key") },
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSalvar(texto) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

@Composable
private fun ResultadoRow(r: TmdbResultado, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaPoster(
            titulo = r.titulo,
            posterUrl = r.posterUrl,
            modifier = Modifier.size(width = 56.dp, height = 80.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                r.titulo,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            val sub = listOfNotNull(r.tipo.rotulo, r.ano).joinToString(" · ")
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (r.generos.isNotEmpty()) {
                Text(
                    r.generos.take(3).joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CentroPreenchido(conteudo: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { conteudo() }
}

@Composable
private fun Aviso(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    detalhe: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icone,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            detalhe,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
