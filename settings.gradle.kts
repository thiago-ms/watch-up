pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WatchUp"

// App host (thin) + módulos. O core concentra domínio e dados (Room); cada tela
// do MVP é um módulo de feature Android Library próprio, para builds
// incrementais/paralelos e manutenção isolada. Features dependem só de :core:*
// (nunca de :app nem umas das outras) — a navegação entre elas vive no :app.
include(":app")
include(":core:ui")
include(":core:data")
include(":feature:home")
include(":feature:library")
include(":feature:search")
include(":feature:detail")
include(":feature:registration")
include(":feature:settings")
