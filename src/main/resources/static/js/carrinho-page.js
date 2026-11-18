import { del, get, put } from './api.js';
import { moeda } from './formatacao.js';
import { menuMobile, atualizarNavegacao, mostrarAviso, escapar } from './ui.js';

const lista = document.querySelector('[data-cart-items]');
const aviso = document.querySelector('[data-notice]');
menuMobile();
atualizarNavegacao();
carregarCarrinho();

async function carregarCarrinho() {
  const carrinho = await get('/api/carrinho').then(resultado => resultado, erro => {
    mostrarAviso(aviso, erro.message || 'Entre na sua conta para acessar o carrinho.');
    lista.innerHTML = '<div class="empty-state">Seu carrinho está disponível depois de entrar na conta.</div>';
    return null;
  });
  if (!carrinho) return;
  renderizar(carrinho);
}

function renderizar(carrinho) {
  const itens = carrinho.itens || [];
  document.querySelector('[data-item-count]').textContent = String(itens.reduce((total, item) => total + item.quantidade, 0));
  document.querySelector('[data-subtotal]').textContent = moeda(carrinho.subtotal);
  const finalizar = document.querySelector('[data-checkout]');
  finalizar.classList.toggle('hidden', itens.length === 0);
  if (!itens.length) {
    lista.innerHTML = '<div class="empty-state">Seu carrinho está vazio. Tem um sabor esperando por você.</div>';
    return;
  }
  lista.innerHTML = itens.map(item => `<article class="cart-row" data-item="${item.id}">
    <div><h3 class="cart-title">${escapar(item.nomeProduto)}</h3><p class="cart-options">${(item.opcoes || []).map(opcao => `${escapar(opcao.tipo)}: ${escapar(opcao.nome)}`).join(' · ') || 'Sem personalização'}</p><p class="cart-options">${moeda(item.precoUnitario)} cada · Subtotal ${moeda(item.subtotal)}</p></div>
    <div class="cart-actions"><label><span class="sr-only">Quantidade de ${escapar(item.nomeProduto)}</span><input class="quantity-input" type="number" min="1" max="50" value="${item.quantidade}" data-quantity></label><button class="text-button" type="button" data-remove aria-label="Remover ${escapar(item.nomeProduto)}">Remover</button></div>
  </article>`).join('');
  lista.querySelectorAll('[data-quantity]').forEach(input => input.addEventListener('change', atualizarItem));
  lista.querySelectorAll('[data-remove]').forEach(botao => botao.addEventListener('click', removerItem));
}

function atualizarItem(event) {
  const itemId = event.currentTarget.closest('[data-item]').dataset.item;
  const quantidade = Number(event.currentTarget.value);
  if (!Number.isInteger(quantidade) || quantidade < 1 || quantidade > 50) {
    mostrarAviso(aviso, 'A quantidade deve ficar entre 1 e 50.');
    carregarCarrinho();
    return;
  }
  put(`/api/carrinho/itens/${itemId}`, { quantidade }).then(renderizar, erro => mostrarAviso(aviso, erro.message));
}

function removerItem(event) {
  const itemId = event.currentTarget.closest('[data-item]').dataset.item;
  del(`/api/carrinho/itens/${itemId}`).then(carregarCarrinho, erro => mostrarAviso(aviso, erro.message));
}