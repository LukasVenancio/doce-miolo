import { get, post, queryString } from './api.js';
import { dataHora, moeda, nomeStatus } from './formatacao.js';
import { menuMobile, atualizarNavegacao, mostrarAviso, escapar } from './ui.js';

const lista = document.querySelector('[data-orders]');
const aviso = document.querySelector('[data-notice]');
const paginacao = document.querySelector('[data-pagination]');
let paginaAtual = 0;
menuMobile();
atualizarNavegacao();
carregarPedidos();

async function carregarPedidos() {
  const pagina = await get(`/api/pedidos${queryString({ page: paginaAtual, size: 10 })}`).then(resultado => resultado, erro => {
    mostrarAviso(aviso, erro.message || 'Entre na sua conta para consultar seus pedidos.');
    lista.innerHTML = '<div class="empty-state">Faça login para ver seu histórico.</div>';
    return null;
  });
  if (!pagina) return;
  if (!pagina.content.length) {
    lista.innerHTML = '<div class="empty-state">Você ainda não fez pedidos. <a href="index.html">Conheça o cardápio.</a></div>';
    paginacao.replaceChildren();
    return;
  }
  lista.innerHTML = pagina.content.map(pedido => cardPedido(pedido)).join('');
  lista.querySelectorAll('[data-detail]').forEach(botao => botao.addEventListener('click', abrirDetalhe));
  lista.querySelectorAll('[data-confirm]').forEach(botao => botao.addEventListener('click', confirmarPagamento));
  lista.querySelectorAll('[data-cancel]').forEach(botao => botao.addEventListener('click', cancelarPedido));
  renderizarPaginacao(pagina);
}

function cardPedido(pedido) {
  const podeConfirmar = pedido.statusPagamento === 'PENDENTE' && ['PIX', 'CARTAO'].includes(pedido.formaPagamento);
  const podeCancelar = pedido.status === 'AGUARDANDO_PAGAMENTO';
  let acoes = '<button class="button button-outline button-small" type="button" data-detail>Detalhes</button>';
  if (podeConfirmar) acoes += '<button class="button button-small" type="button" data-confirm>Confirmar</button>';
  if (podeCancelar) acoes += '<button class="text-button" type="button" data-cancel>Cancelar</button>';
  return `<article class="order-item" data-order="${pedido.id}"><div><h3>Pedido #${pedido.id}</h3><p class="order-subline">${dataHora(pedido.criadoEm)} · ${escapar(pedido.tipoEntrega)} · ${escapar(pedido.formaPagamento)}</p><p class="order-subline">Pagamento: ${nomeStatus(pedido.statusPagamento)}</p></div><div class="order-right"><span class="status" data-status="${pedido.status}">${nomeStatus(pedido.status)}</span><strong>${moeda(pedido.total)}</strong><div>${acoes}</div></div><div class="order-detail hidden" data-order-detail style="grid-column:1/-1"></div></article>`;
}

function abrirDetalhe(event) {
  const card = event.currentTarget.closest('[data-order]');
  const detalhe = card.querySelector('[data-order-detail]');
  if (!detalhe.classList.contains('hidden')) {
    detalhe.classList.add('hidden');
    return;
  }
  get(`/api/pedidos/${card.dataset.order}`).then(pedido => {
    detalhe.innerHTML = `<div class="divider"></div><h3>Itens</h3>${pedido.itens.map(item => `<p class="order-subline">${escapar(item.nomeProduto)} × ${item.quantidade} · ${moeda(item.precoUnitario)}</p>`).join('')}<h3 style="font-size:19px;margin-top:20px">Acompanhamento</h3><div class="timeline">${pedido.historico.map(evento => `<div class="timeline-entry"><strong>${nomeStatus(evento.status)}</strong><time>${dataHora(evento.em)} · ${escapar(evento.autor)}</time></div>`).join('')}</div>`;
    detalhe.classList.remove('hidden');
  }, erro => mostrarAviso(aviso, erro.message));
}

function confirmarPagamento(event) {
  const id = event.currentTarget.closest('[data-order]').dataset.order;
  post(`/api/pedidos/${id}/pagamento/confirmar`, {}).then(carregarPedidos, erro => mostrarAviso(aviso, erro.message));
}

function cancelarPedido(event) {
  const id = event.currentTarget.closest('[data-order]').dataset.order;
  post(`/api/pedidos/${id}/cancelar`, {}).then(carregarPedidos, erro => mostrarAviso(aviso, erro.message));
}

function renderizarPaginacao(pagina) {
  paginacao.replaceChildren();
  if (pagina.totalPages <= 1) return;
  const anterior = document.createElement('button');
  anterior.className = 'button button-outline button-small';
  anterior.textContent = 'Anterior';
  anterior.disabled = pagina.first;
  anterior.addEventListener('click', () => { paginaAtual -= 1; carregarPedidos(); });
  const proxima = document.createElement('button');
  proxima.className = 'button button-outline button-small';
  proxima.textContent = 'Próxima';
  proxima.disabled = pagina.last;
  proxima.addEventListener('click', () => { paginaAtual += 1; carregarPedidos(); });
  paginacao.append(anterior, proxima);
}