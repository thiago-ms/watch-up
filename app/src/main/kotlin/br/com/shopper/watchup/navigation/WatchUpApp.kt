package br.com.shopper.watchup.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.shopper.watchup.feature.detail.DetailScreen
import br.com.shopper.watchup.feature.detail.ProgressScreen
import br.com.shopper.watchup.feature.home.HomeScreen
import br.com.shopper.watchup.feature.library.LibraryScreen
import br.com.shopper.watchup.feature.registration.RegistrationScreen
import br.com.shopper.watchup.feature.search.SearchScreen
import br.com.shopper.watchup.feature.settings.SettingsScreen

/**
 * Casca de navegação (§2): bottom navigation com 4 abas + FAB central de cadastro.
 * Detalhe, Progresso e Cadastro/Edição são destinos empilhados. A barra e o FAB só
 * aparecem nas abas; trocar de aba limpa a pilha (popUpTo no destino inicial).
 */
@Composable
fun WatchUpApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val emAba = currentRoute in TAB_ROUTES

    Scaffold(
        bottomBar = {
            if (emAba) {
                NavigationBar {
                    TabDestination.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.trocarAba(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (emAba) {
                FloatingActionButton(onClick = { navController.navigate(Routes.REGISTRATION_NEW) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Adicionar mídia")
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            // Só reservamos o espaço da barra inferior; o topo (status bar) é tratado
            // pela TopAppBar de cada tela — evita o padding-top duplicado.
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
            // Troca de tela seca (sem animação de transição).
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenDetail = { id -> navController.navigate(Routes.detail(id)) },
                    onAdicionar = { navController.navigate(Routes.REGISTRATION_NEW) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen()
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onOpenDetail = { id -> navController.navigate(Routes.detail(id)) },
                    onAdicionar = { navController.navigate(Routes.REGISTRATION_NEW) },
                    onAbrirConfig = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument(Routes.ARG_MIDIA_ID) { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong(Routes.ARG_MIDIA_ID) ?: return@composable
                DetailScreen(
                    midiaId = id,
                    onBack = { navController.popBackStack() },
                    onEditar = { mid, step -> navController.navigate(Routes.registrationEdit(mid, step)) },
                    onAtualizarProgresso = { navController.navigate(Routes.progress(id)) },
                )
            }
            composable(
                route = Routes.PROGRESS,
                arguments = listOf(navArgument(Routes.ARG_MIDIA_ID) { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong(Routes.ARG_MIDIA_ID) ?: return@composable
                ProgressScreen(midiaId = id, onBack = { navController.popBackStack() })
            }
            composable(Routes.REGISTRATION_NEW) {
                RegistrationScreen(
                    midiaId = null,
                    onBack = { navController.popBackStack() },
                    onSalvo = { id -> navController.aposCadastro(id) },
                )
            }
            composable(
                route = Routes.REGISTRATION_EDIT,
                arguments = listOf(
                    navArgument(Routes.ARG_MIDIA_ID) { type = NavType.LongType },
                    navArgument(Routes.ARG_STEP) { type = NavType.IntType; defaultValue = 0 },
                ),
            ) { entry ->
                val id = entry.arguments?.getLong(Routes.ARG_MIDIA_ID) ?: return@composable
                val step = entry.arguments?.getInt(Routes.ARG_STEP) ?: 0
                RegistrationScreen(
                    midiaId = id,
                    passoInicial = step,
                    onBack = { navController.popBackStack() },
                    // Edição: salvar volta ao Detalhe já existente na pilha.
                    onSalvo = { navController.popBackStack() },
                )
            }
        }
    }
}

/** Troca de aba resetando a pilha da aba (§2 "reseta histórico da aba"). */
private fun NavHostController.trocarAba(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Após salvar um cadastro novo, abre o Detalhe da mídia criada com a pilha
 * reiniciada em Início (§2).
 */
private fun NavHostController.aposCadastro(midiaId: Long) {
    navigate(Routes.detail(midiaId)) {
        popUpTo(Routes.HOME) { inclusive = false }
    }
}
