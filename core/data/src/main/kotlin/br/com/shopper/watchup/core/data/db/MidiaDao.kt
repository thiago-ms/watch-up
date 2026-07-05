package br.com.shopper.watchup.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import br.com.shopper.watchup.core.data.model.EpisodiosTemporada
import br.com.shopper.watchup.core.data.model.Midia
import kotlinx.coroutines.flow.Flow

@Dao
interface MidiaDao {

    @Query("SELECT * FROM midia ORDER BY titulo COLLATE NOCASE")
    fun observarTodas(): Flow<List<Midia>>

    @Query("SELECT * FROM midia WHERE id = :id")
    fun observarPorId(id: Long): Flow<Midia?>

    // Snapshots pontuais para export/import de backup.
    @Query("SELECT * FROM midia")
    suspend fun listarTodas(): List<Midia>

    @Query("SELECT * FROM episodios_temporada")
    suspend fun listarTodosEpisodios(): List<EpisodiosTemporada>

    @Query("DELETE FROM midia")
    suspend fun limparMidias()

    @Query("DELETE FROM episodios_temporada")
    suspend fun limparEpisodios()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(midia: Midia): Long

    @Update
    suspend fun atualizar(midia: Midia)

    @Delete
    suspend fun remover(midia: Midia)

    // --- Episódios por temporada (tabela filha) ---
    @Query("SELECT * FROM episodios_temporada WHERE midiaId = :midiaId ORDER BY temporada")
    fun observarEpisodios(midiaId: Long): Flow<List<EpisodiosTemporada>>

    @Upsert
    suspend fun salvarEpisodios(episodios: EpisodiosTemporada)

    @Query("DELETE FROM episodios_temporada WHERE midiaId = :midiaId")
    suspend fun removerEpisodiosDaMidia(midiaId: Long)
}
