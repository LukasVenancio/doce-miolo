import { get, post } from './api.js';
import { moeda, validaCep } from './formatacao.js';
import { criaPedidoRequest } from './pedido.js';
import { atualizarNavegacao, mostrarAviso, escapar } from './ui.js';

const form = document.querySelector('[data-checkout-form]');
const aviso = form.querySelector('[data-notice]');
const camposEndereco = form.querySelector('[data-address-fields]');
const enderecoInputs = [...camposEndereco.querySelectorAll('input')];
const fretePadrao = 8;
let subtotalAtual = 0;

atualizarNavegacao();
carregarResumo();
form.querySelectorAll('[name="tipoEntrega"]').forEach(input => input.addEventListener('change', atualizarTipoEntrega));
form.addEventListener('submit', finalizarPedido);
atualizarTipoEntrega();

async function carregarResumo() {
  const carrinho = await get('/api/carrinho').then(resultado => resultado, erro => {
    mostrarAviso(aviso, erro.message || 'Entre na sua conta para finalizar o pedido.');
    return null;
  });
  if (!carrinho) return;
  subtotalAtual = Number(carrinho.subtotal);
  document.querySelector('[data-checkout-subtotal]').textContent = moeda(subtotalAtual);
  const lista = document.querySelector('[data-checkout-items]');
  if (!carrinho.itens.length) {
    lista.innerHTML = '<p class="muted">Seu carrinho está vazio.</p>';
    form.querySelector('[type="submit"]').disabled = true;
    return;
  }
  lista.innerHTML = carrinho.itens.map(item => `<div class="summary-line"><span>${escapar(item.nomeProduto)} × ${item.quantidade}</span><strong>${moeda(item.subtotal)}</strong></div>`).join('');
  atualizarTotais();
}

function atualizarTipoEntrega() {
  const entrega = form.querySelector('[name="tipoEntrega"]:checked').value === 'ENTREGA';
  camposEndereco.classList.toggle('hidden', !entrega);
  enderecoInputs.forEach(input => { input.disabled = !entrega; input.required = entrega && input.name !== 'complemento'; });
  const dinheiro = form.querySelector('[data-cash-option] input');
  dinheiro.disabled = !entrega;
  if (!entrega && dinheiro.checked) form.querySelector('[name="formaPagamento"][value="PIX"]').checked = true;
  atualizarTotais();
}

function atualizarTotais() {
  const entrega = form.querySelector('[name="tipoEntrega"]:checked').value === 'ENTREGA';
  let taxa = 0;
  if (entrega) taxa = fretePadrao;
  document.querySelector('[data-shipping]').textContent = moeda(taxa);
  document.querySelector('[data-checkout-total]').textContent = moeda(subtotalAtual + taxa);
}

function finalizarPedido(event) {
  event.preventDefault();
  const dados = new FormData(form);
  if (dados.get('tipoEntrega') === 'ENTREGA' && !validaCep(dados.get('cep'))) {
    mostrarAviso(aviso, 'Confira o CEP informado.');
    return;
  }
  post('/api/pedidos', criaPedidoRequest(dados)).then(mostrarConfirmacao, erro => mostrarAviso(aviso, erro.message));
}

function mostrarConfirmacao(pedido) {
  form.classList.add('hidden');
  const confirmacao = document.querySelector('[data-confirmation]');
  const conteudo = document.querySelector('[data-confirmation-content]');
  confirmacao.classList.remove('hidden');
  let pix = '';
  if (pedido.codigoPix) {
    pix = `<div class="notice notice-info" data-visible="true"><strong>PIX de demonstração</strong><br>${escapar(pedido.codigoPix)}</div>`;
  }
  conteudo.innerHTML = `<p>Pedido <strong>#${pedido.id}</strong> recebido. Status: <strong>${escapar(pedido.status)}</strong>.</p><p class="muted">Total ${moeda(pedido.total)} · ${escapar(pedido.tipoEntrega)}</p>${pix}`;
  window.scrollTo({ top: confirmacao.offsetTop - 90, behavior: 'smooth' });
}