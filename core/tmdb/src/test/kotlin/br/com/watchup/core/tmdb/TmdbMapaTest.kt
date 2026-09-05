package br.com.watchup.core.tmdb

import br.com.watchup.core.data.model.STREAMINGS_DISPONIVEIS
import br.com.watchup.core.data.model.StatusLancEpisodico
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre as traduções TMDB → vocabulário do app (item 17). Só lógica pura: o parser
 * do [TmdbClient] usa `org.json`, que não existe no classpath de unit test.
 */
class TmdbMapaTest {

    // --- status → StatusLancEpisodico ---------------------------------------

    @Test
    fun `status de serie vira status de lancamento`() {
        assertEquals(StatusLancEpisodico.COMPLETA, TmdbMapa.statusEpisodico("Ended"))
        assertEquals(StatusLancEpisodico.CANCELADA, TmdbMapa.statusEpisodico("Canceled"))
        assertEquals(StatusLancEpisodico.LANCANDO, TmdbMapa.statusEpisodico("Returning Series"))
        assertEquals(StatusLancEpisodico.LANCANDO, TmdbMapa.statusEpisodico("In Production"))
        assertEquals(StatusLancEpisodico.VAI_LANCAR, TmdbMapa.statusEpisodico("Planned"))
    }

    @Test
    fun `status desconhecido ou ausente nao inventa nada`() {
        assertNull(TmdbMapa.statusEpisodico(null))
        assertNull(TmdbMapa.statusEpisodico(""))
        assertNull(TmdbMapa.statusEpisodico("Post Production"))
    }

    @Test
    fun `filme so herda o cancelamento`() {
        assertTrue(TmdbMapa.canceladoNaoEpisodico("Canceled"))
        assertFalse(TmdbMapa.canceladoNaoEpisodico("Released"))
        assertFalse(TmdbMapa.canceladoNaoEpisodico(null))
    }

    // --- normalização de streaming ------------------------------------------

    @Test
    fun `nome comercial do TMDB vira o apelido do app`() {
        assertEquals("Prime", TmdbMapa.streaming("Amazon Prime Video"))
        assertEquals("Disney+", TmdbMapa.streaming("Disney Plus"))
        assertEquals("Paramount+", TmdbMapa.streaming("Paramount Plus"))
        assertEquals("Apple TV+", TmdbMapa.streaming("Apple TV Plus"))
        assertEquals("Max", TmdbMapa.streaming("HBO Max"))
    }

    @Test
    fun `nome ja igual ao do app passa direto e ignora caixa`() {
        assertEquals("Netflix", TmdbMapa.streaming("netflix"))
        assertEquals("Crunchyroll", TmdbMapa.streaming("Crunchyroll"))
        assertEquals("Globoplay", TmdbMapa.streaming(" Globoplay "))
    }

    @Test
    fun `provedor fora da lista e descartado em vez de inventado`() {
        assertNull(TmdbMapa.streaming("Looke"))
        assertNull(TmdbMapa.streaming("MUBI"))
        assertNull(TmdbMapa.streaming(""))
        // Os escapes do formulário nunca vêm do autopreenchimento.
        assertNull(TmdbMapa.streaming("Outro"))
        assertNull(TmdbMapa.streaming("Não sei"))
    }

    @Test
    fun `lista preserva a ordem, remove repetido e o que nao casa`() {
        val normalizados = TmdbMapa.streamings(
            listOf("Amazon Prime Video", "Looke", "Netflix", "Netflix Standard with Ads"),
        )
        assertEquals(listOf("Prime", "Netflix"), normalizados)
    }

    @Test
    fun `todo nome normalizado existe em STREAMINGS_DISPONIVEIS`() {
        val entradas = listOf(
            "Netflix", "Amazon Prime Video", "HBO Max", "Disney Plus", "Apple TV Plus",
            "Crunchyroll", "Globoplay", "Paramount Plus", "Star Plus",
        )
        entradas.forEach { provedor ->
            val nome = TmdbMapa.streaming(provedor)
            assertTrue("$provedor → $nome fora do vocabulário", nome in STREAMINGS_DISPONIVEIS)
        }
    }

    // --- datas ---------------------------------------------------------------

    @Test
    fun `data ISO vira ano, digitos e dia da semana`() {
        // 2024-03-15 é uma sexta-feira.
        assertEquals("2024", TmdbMapa.ano("2024-03-15"))
        assertEquals("15032024", TmdbMapa.digitosData("2024-03-15"))
        assertEquals("Sexta", TmdbMapa.diaDaSemana("2024-03-15"))
    }

    @Test
    fun `data ausente ou malformada nao quebra`() {
        assertNull(TmdbMapa.ano(null))
        assertNull(TmdbMapa.ano(""))
        assertNull(TmdbMapa.digitosData("2024"))
        assertNull(TmdbMapa.digitosData("nao-e-data"))
        assertNull(TmdbMapa.diaDaSemana(null))
        assertNull(TmdbMapa.diaDaSemana("2024-13-45"))
    }
}
