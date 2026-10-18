# diegourban.github.io

Site estático em [Hugo](https://gohugo.io/) com o tema [PaperMod](https://github.com/adityatelange/hugo-PaperMod), publicado em <https://diegourban.github.io/> pelo GitHub Pages a cada push no `master`.

Cada post que traz números tem os scripts e os dados brutos em `benchmarks/<slug>/`, para qualquer leitor reproduzir.

## Estrutura

| Caminho | O que é |
|---|---|
| `hugo.toml` | Configuração do site: título, menu, links sociais, opções do tema |
| `content/posts/<slug>/index.md` | Um post (page bundle: imagens do post ficam na mesma pasta) |
| `content/about.md` | Página "About" |
| `benchmarks/<slug>/` | Scripts e dados brutos de cada post; ficam no repositório, não no site |
| `archetypes/default.md` | Modelo de front matter para posts novos |
| `themes/PaperMod/` | O tema, como submódulo git fixado num commit |
| `.github/workflows/hugo.yml` | Build e deploy no GitHub Pages |

## Dia a dia

```bash
git clone --recurse-submodules git@github.com:diegourban/diegourban.github.io.git
hugo server --buildDrafts                 # preview em http://localhost:1313, recarrega ao salvar
hugo new content posts/meu-post/index.md  # novo post, já com draft: true
hugo --gc --minify                        # build de produção em public/ (rascunhos ficam de fora)
```

Um post só é publicado quando o front matter muda para `draft: false` e o commit chega ao `master`.

Num repositório público, o texto de um rascunho fica visível no GitHub mesmo com `draft: true`. Rascunhos que ainda não podem ser lidos ficam num branch local até estarem prontos.

Use SSH para o push: o token HTTPS do `gh` recusa push de arquivos em `.github/workflows/` sem o escopo `workflow`.

## Publicação

O GitHub Pages usa **Settings → Pages → Source = GitHub Actions**. O workflow builda com Hugo e publica a cada push no `master`, ou manualmente em **Actions → Deploy Hugo site to Pages → Run workflow**.

Domínio próprio (opcional): **Settings → Pages → Custom domain**, mais um registro `CNAME` no seu DNS apontando para `diegourban.github.io`.

O site anterior (um currículo de 2017 em Bootstrap) continua no histórico do `master`.

## Atualizar o tema

O PaperMod está fixado no commit `d376885` (2026-08-02). A última tag, v8.0 (2024), não funciona no Hugo 0.168. Para atualizar:

```bash
git -C themes/PaperMod fetch && git -C themes/PaperMod checkout <commit>
hugo server --buildDrafts   # confira se nada quebrou
git add themes/PaperMod && git commit -m "Update PaperMod"
```

Mantenha a versão do Hugo no workflow (`HUGO_VERSION`) igual à da sua máquina (`hugo version`).
