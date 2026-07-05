package br.com.shopper.watchup.feature.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import br.com.shopper.watchup.core.data.repo.BackupPrefs
import br.com.shopper.watchup.core.data.repo.MidiaRepository
import br.com.shopper.watchup.core.ui.component.PushScreenScaffold
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FORMATO_DATA = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

/**
 * Configurações — backup via Storage Access Framework (SAF). O usuário escolhe uma
 * pasta (pode ser do Google Drive); o app grava/lê o `backup.json` nela. Backup
 * manual ("Fazer backup agora") e automático diário (WorkManager), com painel de
 * status. Sem OAuth, sem Drive API, sem rede direta do app.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { MidiaRepository.get(context) }
    val scope = rememberCoroutineScope()

    var pastaUri by remember { mutableStateOf(BackupPrefs.getPastaUri(context)) }
    var ultimoBackup by remember { mutableLongStateOf(BackupPrefs.getUltimoBackup(context)) }
    var ultimaTentativa by remember { mutableLongStateOf(BackupPrefs.getUltimaTentativa(context)) }
    var ultimaTentativaOk by remember { mutableStateOf(BackupPrefs.getUltimaTentativaOk(context)) }
    var autoAtivo by remember { mutableStateOf(BackupPrefs.getAutoAtivo(context)) }
    var salvando by remember { mutableStateOf(false) }
    var confirmarRestauracao by remember { mutableStateOf(false) }
    var confirmarExclusao by remember { mutableStateOf(false) }

    // Estado do trabalho de backup automático (pendente/em progresso).
    val workInfos by WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow(BackupManager.NOME_TRABALHO)
        .collectAsStateWithLifecycle(emptyList())
    val estadoAuto = workInfos.firstOrNull()?.state

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    fun recarregarStatus() {
        ultimoBackup = BackupPrefs.getUltimoBackup(context)
        ultimaTentativa = BackupPrefs.getUltimaTentativa(context)
        ultimaTentativaOk = BackupPrefs.getUltimaTentativaOk(context)
    }

    val seletorPasta = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            BackupPrefs.setPastaUri(context, uri.toString())
            pastaUri = uri.toString()
        }
    }

    if (confirmarRestauracao) {
        AlertDialog(
            onDismissRequest = { confirmarRestauracao = false },
            title = { Text("Restaurar backup?") },
            text = { Text("Isso substitui toda a sua biblioteca atual pelo conteúdo do backup.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarRestauracao = false
                    scope.launch {
                        val json = BackupManager.lerBackup(context)
                        if (json == null) {
                            toast("Nenhum backup encontrado na pasta.")
                        } else {
                            repo.importarJson(json)
                            toast("Backup restaurado")
                        }
                    }
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { confirmarRestauracao = false }) { Text("Cancelar") } },
        )
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Apagar backup?") },
            text = { Text("O arquivo de backup na pasta será removido. Sua biblioteca no app não é afetada.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarExclusao = false
                    scope.launch {
                        val ok = BackupManager.apagarBackup(context)
                        toast(if (ok) "Backup apagado" else "Nenhum backup para apagar.")
                    }
                }) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") } },
        )
    }

    PushScreenScaffold(title = "Configurações", onBack = onBack) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Text("Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Escolha uma pasta (pode ser do Google Drive) para guardar o backup da sua biblioteca.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            // Pasta + status
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val nomePasta = pastaUri?.let { nomeDaPasta(context, it) }
                    Text("Pasta: ${nomePasta ?: "não configurada"}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Último backup: ${if (ultimoBackup > 0) FORMATO_DATA.format(Date(ultimoBackup)) else "nunca"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val emProgresso = salvando || estadoAuto == WorkInfo.State.RUNNING
                    val statusLinha = when {
                        emProgresso -> "Status: fazendo backup…"
                        ultimaTentativa > 0 && !ultimaTentativaOk && ultimaTentativa >= ultimoBackup ->
                            "Status: última tentativa (${FORMATO_DATA.format(Date(ultimaTentativa))}) falhou"
                        autoAtivo && estadoAuto == WorkInfo.State.ENQUEUED -> "Status: automático agendado"
                        else -> null
                    }
                    if (statusLinha != null) {
                        Spacer(Modifier.height(4.dp))
                        val cor = if (statusLinha.contains("falhou")) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(statusLinha, style = MaterialTheme.typography.bodySmall, color = cor)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { seletorPasta.launch(null) }) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (pastaUri == null) "Escolher pasta" else "Trocar pasta")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val temPasta = pastaUri != null

            // Backup automático (toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Backup automático (diário)", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Uma vez por dia, em segundo plano, quando houver rede.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = autoAtivo,
                    enabled = temPasta,
                    onCheckedChange = { on ->
                        autoAtivo = on
                        BackupPrefs.setAutoAtivo(context, on)
                        if (on) BackupManager.agendarDiario(context) else BackupManager.cancelar(context)
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        salvando = true
                        val ok = BackupManager.executarBackup(context)
                        salvando = false
                        recarregarStatus()
                        toast(if (ok) "Backup salvo" else "Não foi possível salvar o backup.")
                    }
                },
                enabled = temPasta && !salvando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (salvando) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Fazer backup agora")
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { confirmarRestauracao = true },
                enabled = temPasta,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Restaurar backup") }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { confirmarExclusao = true },
                enabled = temPasta,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Apagar backup") }
        }
    }
}

private fun nomeDaPasta(context: android.content.Context, treeUri: String): String? =
    runCatching {
        androidx.documentfile.provider.DocumentFile.fromTreeUri(context, Uri.parse(treeUri))?.name
    }.getOrNull()
