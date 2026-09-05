# WatchUp — Backlog de ajustes, melhorias e bugs

Lista numerada de itens para resolver **um a um**. A numeração é estável: escolha
um número e a gente conversa e executa. `Status` começa tudo em `pendente`.

Legenda de tipo: 🐛 bug · ✨ melhoria · 🔧 ajuste

| # | Tipo | Título | Status |
|---|------|--------|--------|
| 1 | 🔧 | Tirar mídias vistas da home | feito (validar no device) |
| 2 | 🔧 | Home: ordenar datas fixas por data de lançamento | feito (validar no device) |
| 3 | ✨ | Reorganizar seções de datas da home (em cartaz + tags temporais) | feito (validar no device) |
| 4 | ✨ | Sugestão de novos episódios para mídias periódicas | feito (validar no device) |
| 5 | 🐛 | Não dá pra atualizar progresso de série com nova temporada | feito (validar no device) |
| 6 | 🐛 | Episódica com dia/hora de lançamento aparece como "sem data" | feito (validar no device) |
| 7 | 🐛 | Atualizar progresso não passa da temporada 1 / atual | feito (validar no device) |
| 8 | ✨ | Cadastro com "intenção de assistir" (rascunho) | feito (validar no device) |
| 9 | ✨ | Archive para limpar a biblioteca ativa | feito (validar no device) |
| 10 | 🔧 | Biblioteca: esconder vistos por padrão | feito (validar no device) |
| 11 | ✨ | Favoritos + filtro na biblioteca | feito (validar no device) |
| 12 | 🔧 | Home: separar episódicas de não episódicas em 4 blocos | feito (validar no device) |
| 13 | 🔧 | Chave do TMDB no backup | feito (validar no device) |
| 14 | 🐛 | Série promovida pela estimativa não habilita progresso nem temporadas | feito (validar no device) |
| 15 | 🔧 | Form: infos pessoais numa etapa final "Sobre você" | feito (validar no device) |
| 16 | 🔧 | Módulo `:core:tmdb` | feito (validar no device) |
| 17 | ✨ | Autopreenchimento do form via API do TMDB (com toggle) | feito (validar no device) |

## Histórico de entregas

Mapa item → versão em que foi entregue (APK debug+release em `dist/` e release no
hub `../.dist/`). **Manter atualizado a cada entrega:** ao concluir um item, subir a
linha correspondente com a versão (`versionName`) usada no build.

| Versão | Itens | Resumo |
|--------|-------|--------|
| 1.9 | 5, 6, 7 | Bugs de progresso/temporada/data (progresso ciente de temporada; episódica semanal deixa de ser "sem data") |
| 1.10 | 1, 2, 10 | Esconder vistos (home + biblioteca) e ordenar datas fixas por lançamento |
| 1.11 | 3, 11 | Seções de data da home (Em cartaz + tags temporais) · favoritos + filtro |
| 1.12 | 9 | Archive (arquivar/desarquivar; tela discreta via Configurações) |
| 1.13 | 4 | Sugestão de novos episódios por cadência (data-base reancorável) |
| 1.14 | 8 | Cadastro "intenção de assistir" (rascunho parcial + modo na biblioteca) |
| 1.15 | 4 | Novos episódios cientes do dia-da-semana (contagem por calendário quando semanal) |
| 1.16 | 3, 4 | Fixes: "vai lançar" com estreia passada não mostra mais tag "Em breve" no bloco "Em cartaz"; stepper −/+ de episódios reancora a data-base da contagem; Detalhe sem título na barra superior |
| 1.17 | 12, 13, 14, 15, 16, 17 | Home em 4 blocos (não episódicas antes das episódicas) · chave do TMDB no backup (`VERSION` 2) · série promovida pela estimativa passa a abrir temporada, herdar o dia da semana da estreia e consertar registros já quebrados · form com etapa final "Sobre você" (contexto + onde assistir) · módulo `:core:tmdb` (cliente saiu de `:feature:search`) · autopreenchimento do cadastro pela API do TMDB, com toggle em Ajustes |

> Migrações de banco acumuladas até a v1.14: schema Room v4 → v8 (favorito, arquivada,
> cadência+data-base, intenção) — todas incrementais, preservando a biblioteca.
> **A v1.17 não mexeu no schema** (segue v8): o autopreenchimento do TMDB não persiste
> `tmdbId`, por decisão de escopo.

> ⚠️ A v1.17 foi publicada **sem gate de aparelho** (pulado a pedido) e **sem commit nem
> tag**. Os seis itens estão marcados "validar no device" ao pé da letra: ninguém abriu
> este APK. O item 17 é o mais exposto — o mapa de nomes de streaming e a inferência do
> dia da semana nunca foram validados contra resposta real da API do TMDB.

---

## 1. 🔧 Tirar mídias vistas da home
Mídias já marcadas como **vistas** não devem aparecer na home.

- Relacionado ao item **10** (mesma ideia de esconder "vistos", mas na biblioteca).

## 2. 🔧 Home: ordenar datas fixas por data de lançamento
Na home, os itens que têm **data fixa** devem ser ordenados pela **data de
lançamento** (mais próxima primeiro).

## 3. ✨ Reorganizar as seções de datas da home
Hoje datas de estreia **no passado** aparecem em "Próximas datas", o que está
errado. Proposta:

- Criar uma seção **"Em cartaz"** para o que já estreou / está no ar.
- **Unificar** as seções "Esta semana" e "Próximas datas" numa **única seção**,
  usando uma **tag** por item indicando a janela temporal:
  - `esta semana`, `semana que vem`, `este mês`, `próximo mês`, `este ano`,
    `próximo ano`, `futuro distante`, `sem data`.
- **Cor da tag por prioridade** (mais perto = mais destaque). `sem data` usa a
  cor mais neutra possível.

**Definido:**
- **"Em cartaz"** é uma **seção** na **home**; nos **outros lugares** vira **tag**.
- Regra de corte das janelas é **por calendário**: semana-calendário,
  mês-calendário e ano-calendário (não janelas móveis de N dias).

## 4. ✨ Sugestão de novos episódios (mídias periódicas)
Para mídias **periódicas/episódicas**, o app deve **contar as semanas desde a
data de criação** da mídia e **sugerir a quantidade de novos episódios**.

- Home e biblioteca: indicação **sutil** (ex.: badge/hint).
- Detalhe da mídia: **ação para atualizar o cadastro** com a nova quantidade de
  episódios.

**Definido:**
- **Cadência padrão: 1 episódio/semana**, com **opção de cadência configurável
  no detalhe da mídia**.
- Novo campo de data **"data-base da contagem"** na mídia, que serve de âncora do
  contador de novos episódios:
  - **Sempre que a quantidade de episódios é atualizada** (pela opção de
    atualizar quantidade **ou** direto pela edição), esse campo recebe a data da
    atualização e **o contador reinicia a partir dela**.
  - O campo **pode ser nulo** quando o status é **"vai lançar"**. Enquanto nulo,
    a contagem usa a **data de lançamento** como base.
  - Se o cadastro **já é criado com status "lançando"**, o campo é salvo **igual
    à data de criação**.
- Fórmula da sugestão: `episódios_sugeridos = último_episódio +
  semanas_desde(data-base) × cadência` (data-base = campo próprio, ou data de
  lançamento quando o campo for nulo).

**Refinamento v1.15 — contagem ciente do dia-da-semana:**
- Quando a **cadência é semanal** e há **dia-da-semana de lançamento** definido, a
  contagem passa a usar as **ocorrências reais** desse dia no calendário, em vez de
  dividir dias corridos pela cadência. Ex.: data-base numa **segunda** com lançamento
  na **terça** já acusa **1 novo episódio na quarta** (antes dava 0).
- O episódio conta como disponível **no próprio dia** de lançamento.
- Sem dia-da-semana, ou cadência não-semanal (diária/quinzenal/mensal), mantém-se a
  estimativa por intervalos de cadência.
- Vale tanto para o ramo com data-base (limite inferior exclusivo) quanto para o
  "vai lançar" estreado, que conta do ep. 1 na estreia (limite inferior inclusivo).

## 5. 🐛 Série com nova temporada não deixa atualizar progresso
Mídia episódica que **vai lançar nova temporada** e que está sendo assistida (em
temporadas anteriores) **não permite atualizar o progresso**.

- Hipótese: a tela de atualizar progresso precisa expor também a **temporada
  atual** (não só a nova).
- Provavelmente relacionado ao item **7**.

## 6. 🐛 Episódica com dia/hora de lançamento vira "sem data definida"
Mídias episódicas **em lançamento** que têm **dia e hora da semana** de novos
episódios estão aparecendo na home como **"sem data definida"** — é bug.

## 7. 🐛 Atualizar progresso não passa da temporada 1 / atual
Ao atualizar progresso, só é possível avançar até a **temporada 1 ou a atual**
(não identificado qual). Precisa investigar e permitir avançar corretamente.

- Provavelmente relacionado aos itens **5** e **6**.

## 8. ✨ Cadastro com "intenção de assistir" (rascunho)
No cadastro, poder marcar como **"intenção de assistir"**:

- Salva **o que já foi preenchido** e **não exige** passar pelas próximas etapas.
- Esses casos **não aparecem** na home nem na biblioteca por padrão.
- Na biblioteca, ter um modo de visualização para **mostrar só** esses casos.

## 9. ✨ Archive para limpar a biblioteca ativa
Poder marcar mídias como **arquivadas**, removendo-as da **biblioteca principal**
("ativa").

- O acesso ao archive deve ficar por um **caminho menos visível** (não uma aba
  principal).

## 10. 🔧 Biblioteca: esconder vistos por padrão
A biblioteca deve **filtrar os vistos** por padrão; só mostrá-los quando o
usuário **clicar para ver os vistos**.

- Relacionado ao item **1** (mesma ideia, na home).

## 11. ✨ Favoritos + filtro na biblioteca
Criar **favoritos** e permitir **filtrar só os favoritos** na biblioteca.

## 12. 🔧 Home: separar episódicas de não episódicas em 4 blocos
Hoje "Em cartaz" mistura filme e série, o que não é legal. Cada janela ganha um bloco
por natureza, e os episódicos vêm depois dos não episódicos.

- Ordem decidida (agrupada por tipo): `Em cartaz` (filmes) → `Próximas datas` (filmes)
  → `No ar` (séries) → `Próximos episódios` (séries).
- Toda a mudança cabe em `HomeScreen.kt:93-106` (onde `emCartaz`/`proximas` nascem do
  `radar`) e `:176-206` (renderização). O discriminador **já existe**:
  `TipoMidia.episodica` (`Enums.kt:11-20`).
- "Episódica" inclui `SERIE`, `ANIME`, `REALITY` e `PROGRAMA` — não só `SERIE`.
- Blocos vazios somem; nenhum dos quatro mantém o placeholder "Nenhum item neste filtro."
- `LancamentoRow` e `SectionHeader` são reusados sem mudança. Nada fora de
  `:feature:home` precisa compilar diferente, e não há migração de Room.

## 13. 🔧 Chave do TMDB no backup
A chave da API do TMDB não sobrevive a um restore — o backup só carrega `midias` e
`episodios`.

- `BackupSerializer.kt:20-64`: campo `tmdbApiKey` no root e `VERSION` **1 → 2**. O
  `fromJson` lê com valor opcional, então backup v1 continua carregando (chave ausente
  = nula).
- **Bloqueio estrutural**: a chave mora em `TmdbConfig` (`:feature:search`, prefs
  `watchup_tmdb`), e `:feature:settings` **não depende** de `:feature:search`. Além
  disso o `MidiaRepositoryImpl` não tem `Context`, então a chave precisa entrar e sair
  por parâmetro (`exportarJson(tmdbApiKey)`, `importarJson` devolvendo a chave).
  `BackupManager` é a única camada do caminho de backup que tem `Context`.
- Decidido: chave em **texto puro** (é de leitura e gratuita, impacto de vazamento
  baixo), salvando **só a digitada pelo usuário** — nunca a efetiva, para não vazar a
  `BuildConfig.TMDB_API_KEY` embutida no build para dentro do JSON.
- Restaurar só grava se o backup trouxer valor **não-vazio**, para um backup v1 não
  apagar a chave atual.

## 14. 🐛 Série promovida pela estimativa não habilita progresso nem temporadas
Depois que a série estreia e a estimativa atualiza os episódios, o botão "Atualizar
progresso" não aparece e a quantidade de temporadas fica ineditável.

- **Causa**: o `copy` de `DetailScreen.kt:219-234` promove para `LANCANDO` mas deixa
  `temporadasDisponiveis` e `temporadaAtual` em `0`. Com zero temporadas,
  `progressoAcessivel` (`MidiaLogic.kt:98-104`) é falso → o card não renderiza
  (`DetailScreen.kt:201`), e o bloco "Episódios por temporada" some (`:244`).
- **Dois casos, tratamento diferente** (decidido):
  - *série nova* (0 temporadas) → assume **1** temporada;
  - *temporada nova* (N temporadas) → vira **N+1**, `temporadaAtual` = N+1 e os
    episódios **recomeçam do zero**. Hoje `DetailScreen.kt:221` faz
    `episodiosDispTempAtual + novos`, somando os da temporada anterior — bug adjacente
    que entra na mesma correção.
- **Dia da semana**: assumir o mesmo dia da semana de `dataPrincipal` e **persistir** em
  `diaLancamento` (não usar só como fallback de cálculo). **Não sobrescrever** se já
  houver valor. Falta o inverso de `diaDaSemanaDe` (`MidiaLogic.kt:123-132`); ele nasce
  em `:core:data`, porque `DIAS_SEMANA` está em `:feature:registration` e feature não
  depende de feature.
- **Correção retroativa**: séries já gravadas em estado inconsistente (`LANCANDO` com 0
  temporadas) precisam ser consertadas também — corrigir só o botão não conserta o
  registro que já existe.
- **Não mexer em `statusUsuario`** (decidido): `progressoAcessivel` também exige
  `ASSISTINDO`, então uma série em "Quero assistir" segue sem card por design.
- Efeito colateral desejado na Home: com `diaLancamento` preenchido, `janelaData`
  (`MidiaLogic.kt:231-236`) passa a classificar como `EM_CARTAZ` em vez de "Sem data".

## 15. 🔧 Form: infos pessoais numa etapa final "Sobre você"
O form mistura informação da obra com informação pessoal. Contexto de consumo e onde
vai assistir passam para uma etapa final.

- Decidido: **etapa única "Sobre você"** juntando os dois; `ONDE_ASSISTIR` deixa de
  existir e o form vai de **6 para 5 etapas**:
  `Tipo → Título → Gênero → Datas e status → Sobre você → Confirmar`.
- Recortar o bloco "Contexto de consumo" de `PassoDetalhes`
  (`RegistrationScreen.kt:364-375`); a etapa `DETALHES`, ao perder o contexto, passa a
  se chamar **"Gênero"**.
- Mover as validações junto: o ramo `DETALHES` de `validarPasso`
  (`RegistrationModel.kt:128`, contexto nulo) e o ramo `ONDE_ASSISTIR` (`:134`).
- **Armadilha que não quebra compilação**: `DetailScreen.kt:82-84` tem
  `ETAPA_DETALHES = 2`, `ETAPA_ONDE_ASSISTIR = 3`, `ETAPA_DATAS_STATUS = 4` — números
  literais que espelham o enum. Mudar a ordem sem renumerar isso faz a ficha
  (`:439-441`) abrir a etapa errada, em silêncio.
- Sem migração de Room: `FormDraft` e `Midia` mantêm os mesmos campos.

## 16. 🔧 Módulo `:core:tmdb`
`TmdbClient`/`TmdbConfig` vivem em `:feature:search`, e a regra do projeto é que feature
não depende de feature — então `:feature:registration` não consegue chamar o cliente
onde ele está. É o pré-requisito do item 17.

- Decidido: **módulo `:core:tmdb` novo**, não empurrar para `:core:data` — mantém
  `:core:data` só com Room e não obriga `buildConfig = true` lá.
- O bloco de resolução da chave do `feature/search/build.gradle.kts:9-27,41`
  (`-PtmdbApiKey` / env / `local.properties` + `buildConfigField`) viaja junto.
- `TmdbConfig` pode ficar em `:feature:search` como wrapper fino que delega, para não
  mexer nos 5 call-sites de `SearchScreen.kt`.

## 17. ✨ Autopreenchimento do form via API do TMDB (com toggle)
Ao ir da busca para o form, buscar os detalhes no TMDB e preencher o máximo possível.
Com um toggle para desligar — desligado, o form funciona exatamente como hoje.

- **Bloqueio nº 1**: `TmdbResultado` (`TmdbClient.kt:11`) **não carrega `id` nem
  `media_type`**, embora o `parsear()` já leia os dois e os descarte. Sem propagá-los
  por `SearchScreen.onSelecionar` → `Routes.registrationPrefill` (`Routes.kt:44-56`) →
  `WatchUpApp.kt:132-140` até a tela, nada mais é implementável.
- Busca em `/tv/{id}?language=pt-BR&append_to_response=watch/providers`, no
  `LaunchedEffect` de `RegistrationScreen.kt:113`, com loading próprio e **falha
  silenciosa** — o form tem que continuar utilizável se o TMDB cair.
- Cobertura real, campo a campo: `ano`/`dataTexto` (`first_air_date`),
  `statusLancEpisodico` (`status`: `Ended`→COMPLETA, `Canceled`→CANCELADA,
  `Returning Series`/`In Production`→LANCANDO, `Planned`→VAI_LANCAR),
  `temporadasDisponiveis` (`number_of_seasons`, filtrando `season_number > 0`),
  `episodiosDispTempAtual` (`seasons[].episode_count`), `generos` (já alinhados a
  pt-BR), `streamings` (`watch/providers.results.BR.flatrate[]`), `diaLancamento`
  (inferido do dia da semana de `next_episode_to_air.air_date`).
- **`horarioLancamento` é impossível** — o TMDB não expõe horário de exibição em nenhum
  endpoint. Continua manual para sempre.
- **Mapa de normalização de streaming necessário**: TMDB devolve `Amazon Prime Video`,
  `Disney Plus`, `Paramount Plus`; `STREAMINGS_DISPONIVEIS` (`Enums.kt:73-91`) usa
  `Prime`, `Disney+`, `Paramount+`. **Não validado contra resposta real da API.**
- Escopo decidido: **só cadastro novo vindo da busca**. Não persiste `tmdbId`, logo
  **nenhuma migração de Room** (schema fica na v8). Sem isso não existe "atualizar do
  TMDB" em mídia já cadastrada — fica para um item futuro.
- O autopreenchimento **nunca** toca `temporadaAtual` nem `ultimoEpisodioVisto`, que são
  progresso pessoal e não disponibilidade.
- Toggle na tela de Ajustes, no padrão do `Switch` de "Backup automático"
  (`SettingsScreen.kt:200-220`), em seção nova.
