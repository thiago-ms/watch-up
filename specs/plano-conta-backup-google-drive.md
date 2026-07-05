# Plano — Conta/Login, Backup no Google Drive e reestruturação de navegação

> Status: **planejado, não implementado.** Documento de referência para execução.
> Base: conversa de refinamento + [`google-drive.md`](google-drive.md).

## Contexto e objetivo

Adicionar **backup no Google Drive** ao WatchUp. O login com Google é **opcional**:
o app deve continuar 100% utilizável sem login. Antes do backup, fazer uma
mudança de UX fundindo **Início + Lançamentos** numa nova Home e adicionar um
item de navegação de **Conta/Login**.

Conformidade (política Shopper): publicar em ferramentas protegidas por **OAuth2
do Google é permitido**. Os dados do backup são a própria biblioteca do usuário
(mídias/episódios) — não há CPF/e-mail/telefone de clientes. Por conservadorismo,
a tela de Conta evita exibir o e-mail (mostra nome ou apenas "Conectado").

## Decisões travadas (confirmadas com o usuário)

1. **Auth:** fazer a **Fase 1 (scaffold)** primeiro — estado de login persistido e
   backup/restauração **locais (JSON)**. O Google Sign-In + Drive reais entram na
   **Fase 2**, quando o OAuth Client ID estiver criado.
2. **Ações da tela Conta (logado):** Fazer backup agora · Restaurar backup ·
   Apagar backups · Sair.
3. **Local do backup no Drive (Fase 2):** `appDataFolder` (pasta oculta, privada
   do app).
4. **Navegação nova:** `Início · Biblioteca · Buscar · Conta` + FAB central.
   Lançamentos deixa de ser aba (vai para a Home).

---

## Fase 1 — agora (compila e roda; tudo local, sem rede)

### 1.1 Navegação
- Bottom nav passa a ter 4 itens: **Início, Biblioteca, Buscar, Conta** + FAB
  central de cadastro (mantido).
- **Remover a aba Lançamentos:** tirar `:feature:launches` do grafo —
  `settings.gradle.kts`, dependência em `app/build.gradle.kts`, rota `LAUNCHES`
  em `Routes.kt`/`TabDestination`, e apagar o módulo (conteúdo migra p/ a Home).
- **Item Conta com estado dinâmico:** o `WatchUpApp` lê o estado de login e
  escolhe ícone/label do último item:
  - deslogado → label **"Entrar"**, ícone de login (ex.: `Icons.Filled.Login`).
  - logado → label **"Conta"**, ícone de conta (ex.: `Icons.Filled.AccountCircle`).
- Nova rota/aba `account` ("account"). Transição de tela continua seca (sem
  animação), como já configurado no `NavHost`.

### 1.2 Home fundida (`:feature:home`)
- Topo: **"Continuar assistindo"** (grade vertical 2 colunas — como está hoje).
- Abaixo: **radar de Lançamentos completo**, migrado de `LaunchesScreen`:
  - filtros por tipo de data (Todos · Estreia cinema · Estreia streaming · Novo
    episódio);
  - 3 seções fixas (Esta semana · Próximas datas · Sem data definida) com
    contagem e texto "Nenhum item neste filtro." quando vazio.
- **Remover** a antiga seção "Estreias desta semana (3 primeiros)" — o radar
  completo a substitui.
- Estado de carregamento/vazio: manter o tratamento atual (não piscar o empty
  state antes da 1ª emissão do Room).
- `onOpenDetail` continua abrindo o Detalhe a partir de qualquer card/linha.

### 1.3 Conta (novo módulo `:feature:account`)
- **Store de login** simples (SharedPreferences, no padrão do `ToggleStore`),
  colocado em **`:core:data`** para ser lido tanto pelo `:app` (ícone da nav)
  quanto pela tela de Conta. Guarda: flag `logado` + (opcional) nome de exibição.
- **Tela deslogado:**
  - botão **"Entrar com Google"** (Fase 1: apenas seta a flag de login — nenhum
    contato real com o Google);
  - texto deixando claro que **login é opcional** e o app funciona sem ele.
- **Tela logado:**
  - identificação discreta ("Conectado" / nome);
  - **Fazer backup agora** · **Restaurar backup** · **Apagar backups** · **Sair**.
- **Backup Fase 1 = JSON local** (sem lib nova, via `org.json`):
  - `exportarJson()`: serializa a biblioteca (todas as `Midia` + `EpisodiosTemporada`);
  - grava em arquivo local do app (ex.: `filesDir/backup.json`);
  - **Restaurar**: lê o JSON e substitui a biblioteca local;
  - **Apagar backups**: remove o arquivo;
  - métodos novos no `MidiaRepository`: `exportarJson()`, `importarJson(json)`,
    e um `apagarBackups()` (ou equivalente no store/serviço de backup).
- **Sem permissão INTERNET** nesta fase (segue offline).

### 1.4 Fecho da Fase 1
- Bump de versão do app + rebuild via Docker (`make`/`gradlew` no container) +
  rodar testes.

---

## Fase 2 — depois (Google Sign-In + Drive reais)

Pré-requisito **do usuário**: criar o **OAuth Client ID Android** no Google Cloud:
- projeto GCP com **Drive API habilitada**;
- **tela de consentimento** configurada;
- OAuth client Android com package **`br.com.watchup`** + **SHA-1** da
  chave de assinatura (o SHA-1 do keystore debug pode ser extraído do container e
  repassado; o registro no GCP é feito pelo usuário).

Implementação:
- Trocar o "login simulado" por **Google Sign-In** real — abordagem leve:
  **Credential Manager** (login) + **`AuthorizationClient`** (play-services-auth)
  para obter token com escopo **`https://www.googleapis.com/auth/drive.appdata`**.
- Trocar o arquivo local pela **`appDataFolder`** do Drive:
  - upload do `backup.json`, download para restaurar, e delete para apagar;
  - chamadas via **Drive REST em HTTP simples** (OkHttp/HttpURLConnection) para
    evitar a lib pesada `google-api-services-drive`.
- Adicionar permissão **INTERNET** (só o backup usa rede).
- Opcional (recomendado pelo `google-drive.md`): mostrar "Último backup" (data e
  tamanho) e, na 1ª execução após reinstalar, oferecer "Restaurar backup".

### Formato do backup (JSON)
Export do banco (mídias + episódios por temporada) com um campo de versão, ex.:
```json
{
  "version": 1,
  "midias": [ /* todos os campos de Midia */ ],
  "episodios": [ /* {midiaId, temporada, quantidade} */ ]
}
```
Serializar/desserializar mantendo compatibilidade entre versões do schema.

---

## Impactos por arquivo (resumo para execução)

- `settings.gradle.kts` — remover `:feature:launches`; incluir `:feature:account`.
- `app/build.gradle.kts` — trocar dependência de `:feature:launches` por
  `:feature:account`; bump de versão.
- `app/.../navigation/Routes.kt` — remover `LAUNCHES`; ajustar `TabDestination`
  (Início, Biblioteca, Buscar, Conta); adicionar rota `account`.
- `app/.../navigation/WatchUpApp.kt` — remover composable de Lançamentos; Home
  passa a mostrar o conteúdo fundido; adicionar composable de Conta; ícone/label
  do item Conta conforme estado de login.
- `:feature:home` — fundir o conteúdo de `LaunchesScreen` na `HomeScreen`.
- `:feature:launches` — **removido**.
- `:feature:account` (novo) — tela de login/conta + ações de backup.
- `:core:data` — store de login (SharedPreferences); métodos de export/import
  JSON e apagar backup no `MidiaRepository`.
- (Fase 2) `app/src/main/AndroidManifest.xml` — permissão INTERNET; deps de
  Google Sign-In/Drive.

## Notas de conformidade
- Google Drive via OAuth2 é alvo permitido pela política de dados da Shopper.
- Nenhum dado de cliente (CPF/e-mail/telefone) é tratado; o backup é a biblioteca
  pessoal do usuário.
- Na Fase 1 nada sai do aparelho (backup local). Rede só entra na Fase 2.
