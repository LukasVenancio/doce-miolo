import { get, post, put, patch, queryString } from './api.js';
import { tokenAtual, perfilAtual } from './auth.js';
import { moeda } from './formatacao.js';
import { menuMobile, mostrarAviso, atualizarNavegacao, escapar } from './ui.js';

const form = document.querySelector('[data-product-form]');
const aviso = document.querySelector('[data-notice]');
const linhas = document.querySelector('[data-product-rows]');
const seletorCategoria = document.querySelector('[data-categories]');
const paginacao = document.querySelector('[data-pagination]');
let paginaAtual = 0;

menuMobile();
atualizarNavegacao();
iniciarPainel();

function iniciarPainel() {
  if (!tokenAtual() || perfilAtual() !== 'ADMIN') {
    window.location.replace('../login.html');
    return;
  }
  carregarCategorias();
  carregarProdutos();
  form.addEventListener('submit', salvarProduto);
  document.querySelector('[data-cancel-edit]').addEventListener('click', limparFormulario);
  document.querySelector('[data-refresh]').addEventListener('click', carregarProdutos);
}

async function carregarCategorias() {
  const categorias = await get('/api/categorias').then(resultado => resultado, erro => mostrarAviso(aviso, erro.message));
  if (!categorias) return;
  seletorCategoria.insertAdjacentHTML('beforeend', categorias.map(categoria => `<option value="${categoria.id}">${escapar(categoria.nome)}</option>`).join(''));
}

async function carregarProdutos() {
  const pagina = await get(`/api/admin/produtos${queryString({ page: paginaAtual, size: 12, sort: 'nome,asc' })}`)
    .then(resultado => resultado, erro => {
      mostrarAviso(aviso, erro.message);
      linhas.innerHTML = '<tr><td colspan="5">Não foi possível carregar os produtos.</td></tr>';
      return null;
    });
  if (!pagina) return;
  linhas.innerHTML = pagina.content.map(linhaProduto).join('');
  linhas.querySelectorAll('[data-edit]').forEach(botao => botao.addEventListener('click', editarProduto));
  linhas.querySelectorAll('[data-disable]').forEach(botao => botao.addEventListener('click', inativarProduto));
  renderizarPaginacao(pagina);
}

function salvarProduto(event) {
  event.preventDefault();
  const dados = new FormData(form);
  const id = dados.get('id');
  const request = {
    categoriaId: Number(dados.get('categoriaId')),
    nome: dados.get('nome').trim(),
    descricao: dados.get('descricao').trim(),
    precoBase: Number(dados.get('precoBase')),
    imagemUrl: dados.get('imagemUrl').trim() || null,
    personalizavel: dados.get('personalizavel') === 'on'
  };
  const operacao = salvarRequest(id, request);
  operacao.then(() => {
    let mensagem = 'Produto cadastrado.';
    if (id) mensagem = 'Produto atualizado.';
    mostrarAviso(aviso, mensagem, 'success');
    limparFormulario();
    carregarProdutos();
  }, erro => mostrarAviso(aviso, erro.message));
}

function editarProduto(event) {
  const produtoId = event.currentTarget.closest('[data-product]').dataset.product;
  get(`/api/admin/produtos/${produtoId}`).then(preencherFormulario, erro => mostrarAviso(aviso, erro.message));
}

function preencherFormulario(produto) {
  form.elements.id.value = produto.id;
  form.elements.nome.value = produto.nome;
  form.elements.descricao.value = produto.descricao;
  form.elements.categoriaId.value = produto.categoriaId;
  form.elements.precoBase.value = produto.precoBase;
  form.elements.imagemUrl.value = produto.imagemUrl || '';
  form.elements.personalizavel.checked = produto.personalizavel;
  document.querySelector('[data-form-title]').textContent = 'Editar cupcake';
  document.querySelector('[data-form-eyebrow]').textContent = `Produto #${produto.id}`;
  document.querySelector('[data-save-button]').textContent = 'Salvar alterações';
  document.querySelector('[data-cancel-edit]').classList.remove('hidden');
  form.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function limparFormulario() {
  form.reset();
  form.elements.id.value = '';
  form.elements.personalizavel.checked = true;
  document.querySelector('[data-form-title]').textContent = 'Cadastrar cupcake';
  document.querySelector('[data-form-eyebrow]').textContent = 'Novo item';
  document.querySelector('[data-save-button]').textContent = 'Cadastrar';
  document.querySelector('[data-cancel-edit]').classList.add('hidden');
}

function inativarProduto(event) {
  const produtoId = event.currentTarget.closest('[data-product]').dataset.product;
  patch(`/api/admin/produtos/${produtoId}/inativar`, {}).then(carregarProdutos, erro => mostrarAviso(aviso, erro.message));
}

function renderizarPaginacao(pagina) {
  paginacao.replaceChildren();
  if (pagina.totalPages <= 1) return;
  const anterior = botaoPagina('Anterior', pagina.first, () => { paginaAtual -= 1; carregarProdutos(); });
  const proxima = botaoPagina('Próxima', pagina.last, () => { paginaAtual += 1; carregarProdutos(); });
  paginacao.append(anterior, proxima);
}

function botaoPagina(texto, desabilitado, acao) {
  const botao = document.createElement('button');
  botao.className = 'button button-outline button-small';
  botao.textContent = texto;
  botao.disabled = desabilitado;
  botao.addEventListener('click', acao);
  return botao;
}

function salvarRequest(id, request) {
  if (id) return put(`/api/admin/produtos/${id}`, request);
  return post('/api/admin/produtos', request);
}

function linhaProduto(produto) {
  let situacao = 'Ativo';
  let acaoInativar = '';
  if (produto.ativo === false) {
    situacao = 'Inativo';
  }
  if (produto.ativo !== false) {
    acaoInativar = '<button class="button button-danger button-small" type="button" data-disable>Inativar</button>';
  }
  return `<tr data-product="${produto.id}"><td><strong>${escapar(produto.nome)}</strong></td><td>${escapar(produto.categoriaNome)}</td><td>${moeda(produto.precoBase)}</td><td><span class="status">${situacao}</span></td><td><button class="button button-outline button-small" type="button" data-edit>Editar</button>${acaoInativar}</td></tr>`;
}