import { get, post } from './api.js';
import { tokenAtual } from './auth.js';
import { moeda } from './formatacao.js';
import { CUPCAKE_IMAGE, menuMobile, atualizarNavegacao, mostrarAviso, escapar } from './ui.js';

const idProduto = new URLSearchParams(window.location.search).get('id');
const form = document.querySelector('[data-product-form]');
const opcoesElemento = document.querySelector('[data-options]');
const aviso = document.querySelector('[data-notice]');
let produto;
let opcoes = [];

menuMobile();
atualizarNavegacao();
carregarProduto();

form.addEventListener('change', atualizarPreco);
form.addEventListener('submit', adicionarAoCarrinho);

async function carregarProduto() {
  if (!idProduto) {
    mostrarAviso(aviso, 'Escolha um cupcake no cardápio para continuar.');
    form.classList.add('hidden');
    return;
  }
  const resultado = await get(`/api/produtos/${encodeURIComponent(idProduto)}`).then(valor => valor, mostrarErro);
  if (!resultado) return;
  produto = resultado;
  preencherProduto();
  if (!produto.personalizavel) {
    opcoesElemento.innerHTML = '<p class="muted">Este cupcake é servido na combinação da casa.</p>';
    return;
  }
  const opcoesResultado = await get('/api/opcoes-personalizacao').then(valor => valor, mostrarErro);
  if (!opcoesResultado) return;
  opcoes = opcoesResultado;
  renderizarOpcoes();
  atualizarPreco();
}

function preencherProduto() {
  document.title = `${produto.nome} | Doce Miolo`;
  document.querySelector('[data-product-title]').textContent = produto.nome;
  document.querySelector('[data-product-category]').textContent = produto.categoriaNome;
  document.querySelector('[data-product-description]').textContent = produto.descricao;
  const imagem = document.querySelector('[data-product-image]');
  imagem.src = produto.imagemUrl || CUPCAKE_IMAGE;
  imagem.alt = `Cupcake ${produto.nome}`;
}

function renderizarOpcoes() {
  const grupos = ['MASSA', 'RECHEIO', 'COBERTURA', 'EXTRA'];
  opcoesElemento.innerHTML = grupos.map(tipo => {
    const grupo = opcoes.filter(opcao => opcao.tipo === tipo);
    if (!grupo.length) return '';
    const multiplos = tipo === 'EXTRA';
    const titulo = tipo.charAt(0) + tipo.slice(1).toLowerCase();
    const controles = grupo.map(opcao => `<label class="option-choice">
      <input type="${tipoControle(multiplos)}" name="opcao-${tipo}" value="${opcao.id}" data-extra="${multiplos}">
      <span>${escapar(opcao.nome)}</span><small>${adicional(opcao.precoAdicional)}</small>
    </label>`).join('');
    return `<fieldset class="option-group"><legend>${titulo}${sufixoOpcional(multiplos)}</legend>${controles}</fieldset>`;
  }).join('');
}

function tipoControle(multiplos) {
  if (multiplos) return 'checkbox';
  return 'radio';
}

function sufixoOpcional(multiplos) {
  if (multiplos) return ' (opcional)';
  return ' *';
}

function adicional(valor) {
  const numero = Number(valor);
  if (numero === 0) return 'incluso';
  return `+ ${moeda(numero)}`;
}

function opcoesSelecionadas() {
  return [...form.querySelectorAll('input[name^="opcao-"]:checked')].map(input => Number(input.value));
}

function atualizarPreco() {
  if (!produto) return;
  const selecionadas = new Set(opcoesSelecionadas());
  const adicionais = opcoes.filter(opcao => selecionadas.has(opcao.id))
    .reduce((total, opcao) => total + Number(opcao.precoAdicional), 0);
  document.querySelector('[data-price]').textContent = moeda(Number(produto.precoBase) + adicionais);
}

function validarOpcoesObrigatorias(ids) {
  const tiposSelecionados = opcoes.filter(opcao => ids.includes(opcao.id)).map(opcao => opcao.tipo);
  return ['MASSA', 'RECHEIO', 'COBERTURA'].every(tipo => tiposSelecionados.includes(tipo));
}

function adicionarAoCarrinho(event) {
  event.preventDefault();
  let ids = [];
  if (produto.personalizavel) ids = opcoesSelecionadas();
  if (produto.personalizavel && !validarOpcoesObrigatorias(ids)) {
    mostrarAviso(aviso, 'Selecione massa, recheio e cobertura para continuar.');
    return;
  }
  if (!tokenAtual()) {
    window.location.href = `login.html?retorno=${encodeURIComponent(window.location.href)}`;
    return;
  }
  const quantidade = Number(new FormData(form).get('quantidade'));
  post('/api/carrinho/itens', { produtoId: produto.id, quantidade, opcoesIds: ids })
    .then(() => { window.location.href = 'carrinho.html'; }, erro => mostrarAviso(aviso, erro.message));
}

function mostrarErro(erro) {
  mostrarAviso(aviso, erro.message || 'Não foi possível carregar este cupcake.');
}