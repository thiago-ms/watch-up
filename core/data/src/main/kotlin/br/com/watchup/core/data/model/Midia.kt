package br.com.watchup.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Entidade única do MVP (§3.2), persistida via Room. Os estados derivados
 * (statusMidia, "Em dia") **não** ficam aqui — são calculados no domínio (§5).
 *
 * Campos condicionais (streamings, datas, disponibilidade e progresso) são
 * nuláveis/zerados quando não se aplicam ao tipo/estado da mídia (§5.1).
 */
@Entity(tableName = "midia")
data class Midia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: TipoMidia,
    val titulo: String,
    val ano: Int? = null,
    val generos: List<String> = emptyList(),
    val contexto: Contexto,
    val posterUrl: String? = null, // pôster do TMDB (quando cadastrada via busca)

    // Onde assistir (modalidade + detalhe)
    val modalidade: Modalidade,
    val streamings: List<String> = emptyList(),
    val streamingPrincipal: String? = null, // ∈ streamings
    val cinemaRede: String? = null,

    // Datas e status de lançamento
    val statusLancEpisodico: StatusLancEpisodico? = null, // só episódica
    val cancelada: Boolean = false, // cancelamento de não-episódica (filme etc.)
    val statusData: StatusData,
    val dataPrincipal: LocalDate? = null, // aceita passado/futuro
    val diaLancamento: String? = null, // só episódica LANCANDO
    val horarioLancamento: String? = null,

    // Disponibilidade episódica (≠ progresso)
    val temporadasDisponiveis: Int = 0,
    val temporadaAtual: Int = 0,
    val episodiosDispTempAtual: Int = 0,

    // Contagem de novos episódios (item 4): cadência em dias entre episódios
    // (7 = semanal) e data-base a partir da qual o contador estima novos episódios.
    // A data-base é reancorada sempre que a quantidade é atualizada; nula até haver
    // âncora (aí cai na data de lançamento).
    val cadenciaDias: Int = 7,
    val dataBaseContagem: LocalDate? = null,

    // Progresso do usuário (só ASSISTINDO)
    val ultimoEpisodioVisto: Int = 0,
    val statusUsuario: StatusUsuario,

    // Marcador de favorito (item 11) — independente de status/tipo.
    val favorito: Boolean = false,

    // Arquivada (item 9) — sai da biblioteca ativa e da Home; visível só no arquivo.
    val arquivada: Boolean = false,

    // Intenção de assistir (item 8) — cadastro parcial/rascunho; some da Home e da
    // biblioteca ativa; a Biblioteca tem um modo que mostra só estes.
    val intencao: Boolean = false,
)

/**
 * Episódios por temporada — tabela filha (§3.2), editável no Detalhe (§6, S016)
 * para complementar a quantidade de episódios após o cadastro. Chave composta
 * (midiaId, temporada).
 */
@Entity(tableName = "episodios_temporada", primaryKeys = ["midiaId", "temporada"])
data class EpisodiosTemporada(
    val midiaId: Long,
    val temporada: Int,
    val quantidade: Int,
)
