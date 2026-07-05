package br.com.watchup.core.data.domain

import br.com.watchup.core.data.model.Contexto
import br.com.watchup.core.data.model.Midia
import br.com.watchup.core.data.model.Modalidade
import br.com.watchup.core.data.model.StatusData
import br.com.watchup.core.data.model.StatusLancEpisodico
import br.com.watchup.core.data.model.StatusMidia
import br.com.watchup.core.data.model.StatusUsuario
import br.com.watchup.core.data.model.TipoMidia
import br.com.watchup.core.data.domain.permiteVisto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Cobre as tabelas de decisão §5 (derivação de statusMidia, "Em dia", visibilidade). */
class MidiaLogicTest {

    private val hoje = LocalDate.of(2025, 1, 15)

    private fun filme(
        statusData: StatusData = StatusData.NAO_APLICA,
        data: LocalDate? = null,
    ) = Midia(
        tipo = TipoMidia.FILME,
        titulo = "X",
        generos = listOf("G"),
        contexto = Contexto.SOZINHO,
        modalidade = Modalidade.CINEMA,
        cinemaRede = "UCI",
        statusData = statusData,
        dataPrincipal = data,
        statusUsuario = StatusUsuario.QUERO_ASSISTIR,
    )

    private fun serie(
        statusLancEp: StatusLancEpisodico?,
        statusUsuario: StatusUsuario = StatusUsuario.QUERO_ASSISTIR,
        disp: Int = 0,
        visto: Int = 0,
    ) = Midia(
        tipo = TipoMidia.SERIE,
        titulo = "S",
        generos = listOf("G"),
        contexto = Contexto.SOZINHO,
        modalidade = Modalidade.STREAMING,
        streamings = listOf("Netflix"),
        streamingPrincipal = "Netflix",
        statusLancEpisodico = statusLancEp,
        statusData = StatusData.NAO_APLICA,
        temporadaAtual = if (disp > 0) 1 else 0,
        episodiosDispTempAtual = disp,
        ultimoEpisodioVisto = visto,
        statusUsuario = statusUsuario,
    )

    // §5.2 -------------------------------------------------------------------
    @Test
    fun `filme com data futura vira EM_BREVE`() {
        val m = filme(StatusData.DEFINIDA, hoje.plusDays(30))
        assertEquals(StatusMidia.EM_BREVE, deriveStatusMidia(m, hoje))
    }

    @Test
    fun `filme com data passada vira EM_CARTAZ`() {
        val m = filme(StatusData.DEFINIDA, hoje.minusDays(30))
        assertEquals(StatusMidia.EM_CARTAZ, deriveStatusMidia(m, hoje))
    }

    @Test
    fun `serie completa vira FINALIZADA`() {
        assertEquals(StatusMidia.FINALIZADA, deriveStatusMidia(serie(StatusLancEpisodico.COMPLETA), hoje))
    }

    @Test
    fun `serie lancando vira LANCANDO`() {
        assertEquals(StatusMidia.LANCANDO, deriveStatusMidia(serie(StatusLancEpisodico.LANCANDO), hoje))
    }

    @Test
    fun `serie que vai lancar vira EM_BREVE`() {
        assertEquals(StatusMidia.EM_BREVE, deriveStatusMidia(serie(StatusLancEpisodico.VAI_LANCAR), hoje))
    }

    @Test
    fun `serie cancelada vira CANCELADA`() {
        assertEquals(StatusMidia.CANCELADA, deriveStatusMidia(serie(StatusLancEpisodico.CANCELADA), hoje))
    }

    @Test
    fun `filme cancelado vira CANCELADA`() {
        val m = filme(StatusData.DEFINIDA, hoje.plusDays(10)).copy(cancelada = true)
        assertEquals(StatusMidia.CANCELADA, deriveStatusMidia(m, hoje))
    }

    @Test
    fun `permiteVisto libera para completa cancelada e nao episodica`() {
        assertTrue(permiteVisto(TipoMidia.SERIE, StatusLancEpisodico.COMPLETA))
        assertTrue(permiteVisto(TipoMidia.SERIE, StatusLancEpisodico.CANCELADA))
        assertFalse(permiteVisto(TipoMidia.SERIE, StatusLancEpisodico.LANCANDO))
        assertTrue(permiteVisto(TipoMidia.FILME, null))
    }

    // §5.3 -------------------------------------------------------------------
    @Test
    fun `assistindo com ultimo visto igual ao disponivel fica em dia`() {
        val m = serie(StatusLancEpisodico.LANCANDO, StatusUsuario.ASSISTINDO, disp = 5, visto = 5)
        assertTrue(estaEmDia(m))
        assertEquals("Em dia", statusExibicao(m))
    }

    @Test
    fun `assistindo abaixo do disponivel nao fica em dia`() {
        val m = serie(StatusLancEpisodico.LANCANDO, StatusUsuario.ASSISTINDO, disp = 5, visto = 3)
        assertFalse(estaEmDia(m))
        assertEquals("Assistindo", statusExibicao(m))
        assertEquals(2, episodiosFaltantes(m))
    }

    // §5.1 -------------------------------------------------------------------
    @Test
    fun `vai lancar oculta disponibilidade e progresso`() {
        assertFalse(disponibilidadeVisivel(TipoMidia.SERIE, StatusLancEpisodico.VAI_LANCAR))
        assertFalse(progressoVisivel(TipoMidia.SERIE, StatusLancEpisodico.VAI_LANCAR, StatusUsuario.ASSISTINDO))
    }

    @Test
    fun `lancando assistindo exibe progresso`() {
        assertTrue(disponibilidadeVisivel(TipoMidia.SERIE, StatusLancEpisodico.LANCANDO))
        assertTrue(progressoVisivel(TipoMidia.SERIE, StatusLancEpisodico.LANCANDO, StatusUsuario.ASSISTINDO))
    }

    @Test
    fun `nao episodica nunca tem progresso`() {
        assertFalse(progressoVisivel(TipoMidia.FILME, null, StatusUsuario.ASSISTINDO))
    }

    // Lançamentos --------------------------------------------------------------
    @Test
    fun `data dentro de 7 dias cai em esta semana`() {
        val m = filme(StatusData.DEFINIDA, hoje.plusDays(3))
        assertEquals(SecaoLancamento.ESTA_SEMANA, secaoLancamento(m, hoje))
    }

    @Test
    fun `data distante cai em proximas datas`() {
        val m = filme(StatusData.DEFINIDA, hoje.plusDays(40))
        assertEquals(SecaoLancamento.PROXIMAS_DATAS, secaoLancamento(m, hoje))
    }

    @Test
    fun `sem data definida cai em sem data`() {
        val m = filme(StatusData.SEM_DATA, null)
        assertEquals(SecaoLancamento.SEM_DATA, secaoLancamento(m, hoje))
    }
}
