package br.com.shopper.watchup.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
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
    const val LAUNCHES = "launches"
    const val SEARCH = "search"
    const val LIBRARY = "library"

    const val DETAIL = "detail/{midiaId}"
    const val PROGRESS = "progress/{midiaId}"
    const val REGISTRATION_NEW = "registration/new"
    const val REGISTRATION_EDIT = "registration/edit/{midiaId}?step={step}"

    const val ARG_MIDIA_ID = "midiaId"
    const val ARG_STEP = "step"

    fun detail(midiaId: Long) = "detail/$midiaId"
    fun progress(midiaId: Long) = "progress/$midiaId"

    /** Edição da mídia abrindo direto numa etapa (0 = início). */
    fun registrationEdit(midiaId: Long, step: Int = 0) = "registration/edit/$midiaId?step=$step"
}

/** Aba do bottom navigation. A ordem define a posição na barra. */
enum class TabDestination(val route: String, val label: String, val icon: ImageVector) {
    HOME(Routes.HOME, "Início", Icons.Filled.Home),
    LAUNCHES(Routes.LAUNCHES, "Lançamentos", Icons.Filled.CalendarMonth),
    SEARCH(Routes.SEARCH, "Buscar", Icons.Filled.Search),
    LIBRARY(Routes.LIBRARY, "Biblioteca", Icons.Filled.VideoLibrary),
}

val TAB_ROUTES: Set<String> = TabDestination.entries.map { it.route }.toSet()
