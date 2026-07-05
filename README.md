# WatchUp (Android)

App Android **multi-módulo** de organização de mídias (biblioteca pessoal,
progresso episódico e radar de lançamentos), implementado a partir da
[spec de desenvolvimento](specs/WatchUp%20-%20Spec%20de%20Desenvolvimento.pdf).

- **Linguagem/UI:** Kotlin + Jetpack Compose (Material 3)
- **Navegação:** Navigation Compose (bottom navigation com 4 abas + FAB central)
- **Persistência:** Room (local-only no MVP; `MidiaRepository` é interface para
  futura sincronização remota sem tocar na UI)
- **Build:** 100% via Docker (não precisa instalar JDK/Gradle/Android SDK no host)
- **Package base:** `br.com.shopper.watchup` · minSdk 26 · targetSdk 35

## Arquitetura de módulos

```
:app                    # host: MainActivity, NavHost, bottom nav + FAB, tema/ícone
:core:ui                # tema (WatchUpTheme) + componentes compartilhados (chips, pôster, progresso)
:core:data              # Room (entidade Midia, DAO, conversores), repositório, seed, domínio (§5) + testes
:feature:home           # S001 — Início (continuar assistindo + estreias da semana)
:feature:launches       # S006 — Lançamentos (radar por status de data)
:feature:search         # S008 — Buscar no catálogo (estados: inicial/carregando/sem resultado/resultados)
:feature:library        # Biblioteca (grade 3 colunas + filtros por tipo)
:feature:detail         # S016 Detalhe + S017 Atualizar progresso
:feature:registration   # S010–S015 Cadastro (6 etapas) + S018 Edição
```

As features dependem apenas de `:core:*` (nunca do `:app` nem umas das outras); a
navegação entre elas vive no `:app`. Estados derivados (statusMidia, "Em dia")
vivem no domínio ([`MidiaLogic`](core/data/src/main/kotlin/br/com/shopper/watchup/core/data/domain/MidiaLogic.kt)),
não na Entity nem na tela.

As versões de dependências ficam centralizadas no version catalog
[`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Modelo de dados

Entidade única `Midia` (Room) + tabela filha `episodios_temporada`. Enums mapeiam
1:1 com o domínio (§3.1). Ver
[`Midia.kt`](core/data/src/main/kotlin/br/com/shopper/watchup/core/data/model/Midia.kt)
e [`Enums.kt`](core/data/src/main/kotlin/br/com/shopper/watchup/core/data/model/Enums.kt).

## Regras de negócio

As tabelas de decisão da spec (§5) estão em
[`MidiaLogic.kt`](core/data/src/main/kotlin/br/com/shopper/watchup/core/data/domain/MidiaLogic.kt)
(derivação de `statusMidia`, estado "Em dia", visibilidade condicional dos blocos
do cadastro) e a validação por etapa (§5.4) em
[`RegistrationModel.kt`](feature/registration/src/main/kotlin/br/com/shopper/watchup/feature/registration/RegistrationModel.kt).
Cobertas por testes unitários
([`MidiaLogicTest`](core/data/src/test/kotlin/br/com/shopper/watchup/core/data/domain/MidiaLogicTest.kt)).

## Build

### Com `make`

```bash
make image     # constrói a imagem Docker de build (1ª vez; baixa SDK/Gradle)
make wrapper   # gera o Gradle wrapper (1ª vez)
make test      # roda os testes unitários (MidiaLogicTest)
make apk       # gera o APK debug
make dist      # APK versionado em dist/watchup-<versão>-debug.apk
```

### Sem `make` (docker compose direto)

```bash
docker compose build
RUN="docker compose run --rm --user $(id -u):$(id -g) android"

$RUN gradle wrapper --gradle-version 8.10.2   # 1ª vez
$RUN ./gradlew --no-daemon testDebugUnitTest  # testes
$RUN ./gradlew --no-daemon assembleDebug      # APK
```

APK gerado em: `app/build/outputs/apk/debug/app-debug.apk`

> **Nota (Fedora/RHEL + SELinux):** o volume é montado com `:Z` no
> `docker-compose.yml` para o relabel do SELinux.

## Instalar (aparelho físico)

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Conformidade

Não trata dados de clientes (CPF/e-mail/telefone). O app é local-only: a
biblioteca é persistida via Room no aparelho e a "busca no catálogo" usa um
catálogo em memória (sem rede). As imagens Docker são apenas consumidas de
registries públicos.
