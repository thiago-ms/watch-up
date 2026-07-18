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
        temporadasDisp: Int = if (disp > 0) 1 else 0,
        diaLancamento: String? = null,
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
        diaLancamento = diaLancamento,
        temporadasDisponiveis = temporadasDisp,
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

    // Janelas temporais (item 3) ---------------------------------------------
    // hoje = quarta 2025-01-15. Semana (domingo→sábado): 12–18/jan; próxima: 19–25/jan.
    @Test
    fun `data passada fica em cartaz`() {
        val m = filme(StatusData.DEFINIDA, hoje.minusDays(2))
        assertEquals(JanelaData.EM_CARTAZ, janelaData(m, hoje))
    }

    @Test
    fun `data nesta semana-calendario cai em esta semana`() {
        val m = filme(StatusData.DEFINIDA, LocalDate.of(2025, 1, 18)) // sábado desta semana
        assertEquals(JanelaData.ESTA_SEMANA, janelaData(m, hoje))
    }

    @Test
    fun `data na proxima semana-calendario cai em semana que vem`() {
        val m = filme(StatusData.DEFINIDA, LocalDate.of(2025, 1, 20)) // segunda da semana seguinte
        assertEquals(JanelaData.SEMANA_QUE_VEM, janelaData(m, hoje))
    }

    @Test
    fun `data ainda neste mes cai em este mes`() {
        val m = filme(StatusData.DEFINIDA, LocalDate.of(2025, 1, 28))
        assertEquals(JanelaData.ESTE_MES, janelaData(m, hoje))
    }

    @Test
    fun `data no mes seguinte cai em proximo mes`() {
        val m = filme(StatusData.DEFINIDA, LocalDate.of(2025, 2, 10))
        assertEquals(JanelaData.PROXIMO_MES, janelaData(m, hoje))
    }

    @Test
    fun `data mais adiante no ano cai em este ano`() {
        val m = filme(StatusData.DEFINIDA, LocalDate.of(2025, 6, 1))
        assertEquals(JanelaData.ESTE_ANO, janelaData(m, hoje))
    }

    @Test
    fun `data no ano seguinte cai em proximo ano`() {
        val m = filme(StatusData.DEFINIDA, LocalDate.of(2026, 3, 1))
        assertEquals(JanelaData.PROXIMO_ANO, janelaData(m, hoje))
    }

    @Test
    fun `data muito distante cai em futuro distante`() {
        val m = filme(StatusData.DEFINIDA, LocalDate.of(2030, 1, 1))
        assertEquals(JanelaData.FUTURO_DISTANTE, janelaData(m, hoje))
    }

    @Test
    fun `sem data definida cai em sem data`() {
        val m = filme(StatusData.SEM_DATA, null)
        assertEquals(JanelaData.SEM_DATA, janelaData(m, hoje))
    }

    @Test
    fun `episodica lancando com dia da semana fica em cartaz`() {
        // Bug: cadência semanal (dia definido) sem dataPrincipal caía em "Sem data".
        val m = serie(StatusLancEpisodico.LANCANDO, disp = 8, visto = 3, diaLancamento = "Quinta")
        assertEquals(JanelaData.EM_CARTAZ, janelaData(m, hoje))
    }

    @Test
    fun `episodica lancando sem dia da semana continua sem data`() {
        val m = serie(StatusLancEpisodico.LANCANDO, disp = 8, visto = 3, diaLancamento = null)
        assertEquals(JanelaData.SEM_DATA, janelaData(m, hoje))
    }

    // Progresso acessível (itens 5 e 7) --------------------------------------
    @Test
    fun `vai lancar temporada nova assistida libera progresso`() {
        // Nova temporada a caminho, mas há temporadas anteriores sendo assistidas.
        val m = serie(
            StatusLancEpisodico.VAI_LANCAR,
            statusUsuario = StatusUsuario.ASSISTINDO,
            temporadasDisp = 2,
        )
        assertTrue(progressoAcessivel(m))
        assertTrue(temCardProgresso(m))
    }

    @Test
    fun `vai lancar serie nova sem temporadas nao tem progresso`() {
        val m = serie(
            StatusLancEpisodico.VAI_LANCAR,
            statusUsuario = StatusUsuario.ASSISTINDO,
            temporadasDisp = 0,
        )
        assertFalse(progressoAcessivel(m))
    }

    @Test
    fun `lancando assistindo com temporadas tem card de progresso`() {
        val m = serie(StatusLancEpisodico.LANCANDO, statusUsuario = StatusUsuario.ASSISTINDO, disp = 10)
        assertTrue(progressoAcessivel(m))
    }

    // Novos episódios (item 4) --------------------------------------------------
    @Test
    fun `lancando estima novos episodios pela cadencia semanal`() {
        // Âncora 3 semanas atrás, cadência semanal → 3 novos episódios estimados.
        val m = serie(StatusLancEpisodico.LANCANDO, disp = 8)
            .copy(cadenciaDias = 7, dataBaseContagem = hoje.minusWeeks(3))
        assertEquals(3, novosEpisodiosEstimados(m, hoje))
    }

    @Test
    fun `sem data-base nao estima novos episodios`() {
        val m = serie(StatusLancEpisodico.LANCANDO, disp = 8).copy(dataBaseContagem = null)
        assertEquals(0, novosEpisodiosEstimados(m, hoje))
    }

    @Test
    fun `data-base no mesmo dia nao estima novidade`() {
        val m = serie(StatusLancEpisodico.LANCANDO, disp = 8).copy(dataBaseContagem = hoje)
        assertEquals(0, novosEpisodiosEstimados(m, hoje))
    }

    @Test
    fun `completa nao estima novos episodios`() {
        val m = serie(StatusLancEpisodico.COMPLETA, disp = 8).copy(dataBaseContagem = hoje.minusWeeks(5))
        assertEquals(0, novosEpisodiosEstimados(m, hoje))
    }

    @Test
    fun `cadencia quinzenal reduz a contagem`() {
        val m = serie(StatusLancEpisodico.LANCANDO, disp = 8)
            .copy(cadenciaDias = 14, dataBaseContagem = hoje.minusWeeks(6)) // 6 semanas / 2 = 3
        assertEquals(3, novosEpisodiosEstimados(m, hoje))
    }

    @Test
    fun `vai lancar com estreia passada conta a partir do ep 1`() {
        // Estreou há 2 semanas, cadência semanal, ainda sem episódios registrados.
        val m = serie(StatusLancEpisodico.VAI_LANCAR, disp = 0)
            .copy(statusData = StatusData.DEFINIDA, dataPrincipal = hoje.minusWeeks(2), dataBaseContagem = null)
        assertEquals(3, novosEpisodiosEstimados(m, hoje)) // ep 1 na estreia + 2 semanas
    }
}
