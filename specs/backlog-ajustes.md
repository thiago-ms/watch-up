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

> Migrações de banco acumuladas até a v1.14: schema Room v4 → v8 (favorito, arquivada,
> cadência+data-base, intenção) — todas incrementais, preservando a biblioteca.

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
