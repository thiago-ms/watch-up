package br.com.watchup.core.data.repo

import android.content.Context
import androidx.room.Room
import br.com.watchup.core.data.db.MIGRATION_4_5
import br.com.watchup.core.data.db.MIGRATION_5_6
import br.com.watchup.core.data.db.MIGRATION_6_7
import br.com.watchup.core.data.db.MIGRATION_7_8
import br.com.watchup.core.data.db.MidiaDao
import br.com.watchup.core.data.db.WatchUpDatabase
import br.com.watchup.core.data.domain.BackupSerializer
import br.com.watchup.core.data.model.EpisodiosTemporada
import br.com.watchup.core.data.model.Midia
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Contrato de dados do app. Mantido como interface (§3.2, "Camadas") para permitir
 * uma futura sincronização remota (Retrofit) sem tocar na UI. No MVP a única
 * implementação é [RoomMidiaRepository] (persistência local via Room).
 *
 * Atualizar progresso é apenas [salvar] com a mídia copiada (novo
 * `ultimoEpisodioVisto`) — o estado "Em dia" é derivado em runtime, nunca
 * persistido (§5.3).
 */
interface MidiaRepository {
    fun observarTodas(): Flow<List<Midia>>
    fun observarPorId(id: Long): Flow<Midia?>
    fun observarEpisodios(midiaId: Long): Flow<List<EpisodiosTemporada>>

    /** Insere (id == 0) ou atualiza (mesmo id) a mídia; devolve o id resultante. */
    suspend fun salvar(midia: Midia): Long
    suspend fun remover(midia: Midia)
    suspend fun salvarEpisodios(episodios: EpisodiosTemporada)

    /**
     * Item 4 — atualiza a quantidade de episódios de uma temporada e, quando é a
     * **temporada atual**, sincroniza a mídia: reflete a quantidade em
     * `episodiosDispTempAtual` e **reancora `dataBaseContagem`** (a contagem de novos
     * episódios reinicia a partir de [hoje]). Editar temporadas passadas não mexe na
     * âncora. Centraliza os pontos que alteram a contagem (steppers −/+ do Detalhe e
     * da tela de progresso).
     */
    suspend fun atualizarEpisodios(
        midia: Midia,
        temporada: Int,
        quantidade: Int,
        hoje: LocalDate = LocalDate.now(),
    )

    /** Backup: serializa toda a biblioteca (mídias + episódios) em JSON. */
    suspend fun exportarJson(): String

    /** Restauração: substitui toda a biblioteca pelo conteúdo do JSON. */
    suspend fun importarJson(json: String)

    companion object {
        @Volatile
        private var instance: MidiaRepository? = null

        /** Singleton de app: DB local + repositório. Semeia na 1ª execução. */
        fun get(context: Context): MidiaRepository =
            instance ?: synchronized(this) {
                instance ?: run {
                    val db = Room.databaseBuilder(
                        context.applicationContext,
                        WatchUpDatabase::class.java,
                        "watchup.db",
                    )
                        // Migrações explícitas onde vale preservar dados; nos demais
                        // upgrades sem migração registrada, recria o banco.
                        .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                        .fallbackToDestructiveMigration()
                        .build()
                    RoomMidiaRepository(db.midiaDao()).also { instance = it }
                }
            }
    }
}

class RoomMidiaRepository(
    private val dao: MidiaDao,
) : MidiaRepository {

    // A biblioteca inicia vazia: os itens são só os que o usuário cadastrar.

    override fun observarTodas(): Flow<List<Midia>> = dao.observarTodas()

    override fun observarPorId(id: Long): Flow<Midia?> = dao.observarPorId(id)

    override fun observarEpisodios(midiaId: Long): Flow<List<EpisodiosTemporada>> =
        dao.observarEpisodios(midiaId)

    override suspend fun salvar(midia: Midia): Long =
        if (midia.id == 0L) {
            dao.inserir(midia)
        } else {
            dao.atualizar(midia)
            midia.id
        }

    override suspend fun remover(midia: Midia) = dao.remover(midia)

    override suspend fun salvarEpisodios(episodios: EpisodiosTemporada) =
        dao.salvarEpisodios(episodios)

    override suspend fun atualizarEpisodios(
        midia: Midia,
        temporada: Int,
        quantidade: Int,
        hoje: LocalDate,
    ) {
        dao.salvarEpisodios(EpisodiosTemporada(midia.id, temporada, quantidade))
        // Só a temporada atual alimenta a contagem de novos episódios (item 4).
        if (temporada == midia.temporadaAtual.coerceAtLeast(1)) {
            dao.atualizar(
                midia.copy(
                    episodiosDispTempAtual = quantidade,
                    dataBaseContagem = hoje,
                ),
            )
        }
    }

    override suspend fun exportarJson(): String =
        BackupSerializer.toJson(dao.listarTodas(), dao.listarTodosEpisodios())

    override suspend fun importarJson(json: String) {
        val (midias, episodios) = BackupSerializer.fromJson(json)
        dao.limparEpisodios()
        dao.limparMidias()
        midias.forEach { dao.inserir(it) }
        episodios.forEach { dao.salvarEpisodios(it) }
    }
}
