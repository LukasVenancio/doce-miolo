import { get, queryString } from './api.js';
import { menuMobile, atualizarNavegacao, cardProduto, escapar } from './ui.js';

const produtosElemento = document.querySelector('[data-products]');
const buscaElemento = document.querySelector('[data-search]');
const categoriaElemento = document.querySelector('[data-category]');
const paginacaoElemento = document.querySelector('[data-pagination]');
let paginaAtual = 0;
let temporizadorBusca;

menuMobile();
atualizarNavegacao();
carregarCategorias();
carregarProdutos();

buscaElemento.addEventListener('input', () => {
  window.clearTimeout(temporizadorBusca);
  temporizadorBusca = window.setTimeout(() => {
    paginaAtual = 0;
    carregarProdutos();
  }, 250);
});

categoriaElemento.addEventListener('change', () => {
  paginaAtual = 0;
  carregarProdutos();
});

async function carregarCategorias() {
  const categorias = await get('/api/categorias').then(resultado => resultado, () => []);
  categoriaElemento.insertAdjacentHTML('beforeend', categorias
    .map(categoria => `<option value="${categoria.id}">${escapar(categoria.nome)}</option>`)
    .join(''));
}

async function carregarProdutos() {
  produtosElemento.innerHTML = '<div class="loading">Buscando sabores...</div>';
  paginacaoElemento.replaceChildren();
  const query = queryString({
    busca: buscaElemento.value.trim(),
    categoriaId: categoriaElemento.value,
    page: paginaAtual,
    size: 9,
    sort: 'nome,asc'
  });
  const pagina = await get(`/api/produtos${query}`).then(resultado => resultado, exibirErro);
  if (!pagina) return;
  renderizarProdutos(pagina.content || []);
  renderizarPaginacao(pagina);
}

function renderizarProdutos(produtos) {
  if (!produtos.length) {
    produtosElemento.innerHTML = '<div class="empty-state" style="grid-column:1/-1">Nenhum sabor encontrado com esses filtros.</div>';
    return;
  }
  produtosElemento.innerHTML = produtos.map(cardProduto).join('');
}

function renderizarPaginacao(pagina) {
  if (pagina.totalPages <= 1) return;
  const anterior = document.createElement('button');
  anterior.className = 'button button-outline button-small';
  anterior.textContent = 'Anterior';
  anterior.disabled = pagina.first;
  anterior.addEventListener('click', () => mudarPagina(pagina.number - 1));
  const proxima = document.createElement('button');
  proxima.className = 'button button-outline button-small';
  proxima.textContent = 'Próxima';
  proxima.disabled = pagina.last;
  proxima.addEventListener('click', () => mudarPagina(pagina.number + 1));
  const indicador = document.createElement('span');
  indicador.className = 'muted';
  indicador.style.alignSelf = 'center';
  indicador.textContent = `${pagina.number + 1} / ${pagina.totalPages}`;
  paginacaoElemento.append(anterior, indicador, proxima);
}

function mudarPagina(numero) {
  paginaAtual = numero;
  carregarProdutos();
  document.querySelector('#cardapio').scrollIntoView({ behavior: 'smooth' });
}

function exibirErro(erro) {
  produtosElemento.innerHTML = `<div class="empty-state" style="grid-column:1/-1">${escapar(erro.message || 'Não foi possível carregar o cardápio.')}</div>`;
}