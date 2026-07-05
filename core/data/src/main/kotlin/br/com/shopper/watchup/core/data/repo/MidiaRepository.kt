package br.com.shopper.watchup.core.data.repo

import android.content.Context
import androidx.room.Room
import br.com.shopper.watchup.core.data.db.MidiaDao
import br.com.shopper.watchup.core.data.db.WatchUpDatabase
import br.com.shopper.watchup.core.data.model.EpisodiosTemporada
import br.com.shopper.watchup.core.data.model.Midia
import kotlinx.coroutines.flow.Flow

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
                    ).build()
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
}
