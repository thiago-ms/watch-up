package br.com.shopper.watchup.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.shopper.watchup.core.data.domain.estaEmDia
import br.com.shopper.watchup.core.data.domain.statusExibicao
import br.com.shopper.watchup.core.data.model.Midia
import br.com.shopper.watchup.core.data.model.StatusMidia
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Casca padrão de uma tela empilhada (Detalhe, Progresso, Cadastro): [TopAppBar]
 * com título e seta de voltar. As 4 abas têm sua própria estrutura no :app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushScreenScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = actions,
            )
        },
        content = content,
    )
}

/** Cabeçalho de seção reutilizável (Home, Lançamentos, Biblioteca). */
@Composable
fun SectionHeader(title: String, trailing: String? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Pôster placeholder: bloco colorido derivado do título, com a inicial. */
@Composable
fun MediaPoster(titulo: String, modifier: Modifier = Modifier) {
    val paleta = listOf(
        Color(0xFF5B4BE8), Color(0xFF00897B), Color(0xFFD81B60),
        Color(0xFF3949AB), Color(0xFFF4511E), Color(0xFF6D4C41),
    )
    val cor = paleta[(titulo.hashCode() % paleta.size + paleta.size) % paleta.size]
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(cor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = titulo.trim().take(1).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Chip do status da mídia (contorno) — "Status da mídia" no Detalhe (§6, S016). */
@Composable
fun StatusMidiaChip(status: StatusMidia, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            status.rotulo,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/**
 * Chip do status do usuário (preenchido) — "Seu status" no Detalhe. Usa o rótulo
 * derivado, exibindo "Em dia" quando aplicável (§5.3).
 */
@Composable
fun StatusUsuarioChip(midia: Midia, modifier: Modifier = Modifier) {
    val emDia = estaEmDia(midia)
    val cor = if (emDia) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = cor) {
        Text(
            statusExibicao(midia),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/** Barra de progresso com rótulo "visto / disponível". */
@Composable
fun LabeledProgressBar(
    fracao: Float,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { fracao },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Estado vazio genérico: ícone + mensagem + CTA opcional (§8). */
@Composable
fun EmptyState(
    mensagem: String,
    modifier: Modifier = Modifier,
    cta: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Movie,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            mensagem,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (cta != null) {
            Spacer(Modifier.height(16.dp))
            cta()
        }
    }
}

private val FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))

/** Formata uma data para exibição; devolve "A definir" quando ausente. */
fun formatarData(data: LocalDate?): String = data?.format(FORMATO_DATA) ?: "A definir"
