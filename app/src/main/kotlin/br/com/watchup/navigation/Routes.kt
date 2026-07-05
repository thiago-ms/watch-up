package br.com.watchup.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Rotas de navegação (§2). As 4 primeiras são abas (bottom navigation); as demais
 * são destinos empilhados (push) sobre a aba atual.
 */
object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"

    const val SETTINGS = "settings"
    const val DETAIL = "detail/{midiaId}"
    const val PROGRESS = "progress/{midiaId}"

    /** Cadastro novo. A rota aceita prefill opcional vindo da busca (query args). */
    const val REGISTRATION_NEW = "registration/new"
    const val REGISTRATION_NEW_ROUTE =
        "registration/new?titulo={titulo}&ano={ano}&tipo={tipo}&generos={generos}&poster={poster}"
    const val REGISTRATION_EDIT = "registration/edit/{midiaId}?step={step}"

    const val ARG_MIDIA_ID = "midiaId"
    const val ARG_STEP = "step"
    const val ARG_TITULO = "titulo"
    const val ARG_ANO = "ano"
    const val ARG_TIPO = "tipo"
    const val ARG_GENEROS = "generos"
    const val ARG_POSTER = "poster"

    fun detail(midiaId: Long) = "detail/$midiaId"
    fun progress(midiaId: Long) = "progress/$midiaId"

    /** Edição da mídia abrindo direto numa etapa (0 = início). */
    fun registrationEdit(midiaId: Long, step: Int = 0) = "registration/edit/$midiaId?step=$step"

    /** Cadastro novo pré-preenchido a partir de um resultado da busca. */
    fun registrationPrefill(
        titulo: String,
        ano: String?,
        tipoNome: String,
        generos: List<String>,
        posterUrl: String?,
    ): String {
        fun enc(s: String?) = Uri.encode(s.orEmpty())
        // Gêneros: nomes juntos por "|" (não aparece em nome de gênero).
        return "registration/new?titulo=${enc(titulo)}&ano=${enc(ano)}" +
            "&tipo=${enc(tipoNome)}&generos=${enc(generos.joinToString("|"))}&poster=${enc(posterUrl)}"
    }
}

/** Aba do bottom navigation. A ordem define a posição na barra. */
enum class TabDestination(val route: String, val label: String, val icon: ImageVector) {
    HOME(Routes.HOME, "Início", Icons.Filled.Home),
    LIBRARY(Routes.LIBRARY, "Biblioteca", Icons.Filled.VideoLibrary),
    SEARCH(Routes.SEARCH, "Buscar", Icons.Filled.Search),
}

val TAB_ROUTES: Set<String> = TabDestination.entries.map { it.route }.toSet()
