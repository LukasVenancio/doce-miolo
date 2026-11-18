import { get, patch, queryString } from './api.js';
import { perfilAtual, tokenAtual } from './auth.js';
import { dataHora, moeda, nomeStatus } from './formatacao.js';
import { menuMobile, atualizarNavegacao, mostrarAviso, escapar } from './ui.js';

const linhas = document.querySelector('[data-order-rows]');
const aviso = document.querySelector('[data-notice]');
const filtro = document.querySelector('[data-status-filter]');
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
  carregarPedidos();
  filtro.addEventListener('change', () => { paginaAtual = 0; carregarPedidos(); });
  document.querySelector('[data-refresh]').addEventListener('click', carregarPedidos);
}

async function carregarPedidos() {
  const parametros = { page: paginaAtual, size: 15, sort: 'criadoEm,desc' };
  if (filtro.value) parametros.status = filtro.value;
  const pagina = await get(`/api/admin/pedidos${queryString(parametros)}`).then(resultado => resultado, erro => {
    mostrarAviso(aviso, erro.message);
    linhas.innerHTML = '<tr><td colspan="6">Não foi possível carregar os pedidos.</td></tr>';
    return null;
  });
  if (!pagina) return;
  if (!pagina.content.length) {
    linhas.innerHTML = '<tr><td colspan="6">Nenhum pedido nesta situação.</td></tr>';
    paginacao.replaceChildren();
    return;
  }
  linhas.innerHTML = pagina.content.map(pedido => `<tr data-order="${pedido.id}" data-delivery="${pedido.tipoEntrega}" data-payment="${pedido.formaPagamento}" data-payment-status="${pedido.statusPagamento}" data-current-status="${pedido.status}">
    <td><strong>#${pedido.id}</strong><br><small>${dataHora(pedido.criadoEm)}</small></td>
    <td>${escapar(pedido.tipoEntrega)}</td><td>${escapar(pedido.formaPagamento)}<br><small>${nomeStatus(pedido.statusPagamento)}</small></td><td>${moeda(pedido.total)}</td><td><span class="status" data-status="${pedido.status}">${nomeStatus(pedido.status)}</span></td><td data-status-cell></td>
  </tr>`).join('');
  linhas.querySelectorAll('[data-order]').forEach(renderizarControleStatus);
  renderizarPaginacao(pagina);
}

function transicoesPossiveis(linha) {
  const status = linha.dataset.currentStatus;
  const transicoes = {
    AGUARDANDO_PAGAMENTO: ['CANCELADO'],
    PAGO: ['EM_PREPARO', 'CANCELADO'],
    EM_PREPARO: [],
    SAIU_PARA_ENTREGA: ['ENTREGUE'],
    ENTREGUE: [],
    CANCELADO: []
  };
  if (status === 'AGUARDANDO_PAGAMENTO' && linha.dataset.payment === 'DINHEIRO' && linha.dataset.paymentStatus === 'PENDENTE') {
    transicoes.AGUARDANDO_PAGAMENTO.unshift('PAGO');
  }
  if (status === 'EM_PREPARO' && linha.dataset.delivery === 'ENTREGA') {
    transicoes.EM_PREPARO = ['SAIU_PARA_ENTREGA'];
  }
  if (status === 'EM_PREPARO' && linha.dataset.delivery === 'RETIRADA') {
    transicoes.EM_PREPARO = ['ENTREGUE'];
  }
  return transicoes[status] || [];
}

function renderizarControleStatus(linha) {
  const opcoes = transicoesPossiveis(linha);
  const celula = linha.querySelector('[data-status-cell]');
  if (!opcoes.length) {
    celula.textContent = 'Sem próximas ações';
    return;
  }
  const seletor = document.createElement('select');
  seletor.className = 'select';
  seletor.setAttribute('aria-label', `Próximo status do pedido ${linha.dataset.order}`);
  seletor.innerHTML = '<option value="">Escolher…</option>' + opcoes.map(status => `<option value="${status}">${nomeStatus(status)}</option>`).join('');
  const botao = document.createElement('button');
  botao.className = 'button button-small';
  botao.type = 'button';
  botao.textContent = 'Atualizar';
  botao.addEventListener('click', () => atualizarStatus(linha, seletor.value));
  celula.append(seletor, botao);
}

function atualizarStatus(linha, status) {
  if (!status) {
    mostrarAviso(aviso, 'Escolha o próximo status.');
    return;
  }
  patch(`/api/admin/pedidos/${linha.dataset.order}/status`, { status })
    .then(carregarPedidos, erro => mostrarAviso(aviso, erro.message));
}

function renderizarPaginacao(pagina) {
  paginacao.replaceChildren();
  if (pagina.totalPages <= 1) return;
  paginacao.append(criarBotao('Anterior', pagina.first, () => { paginaAtual -= 1; carregarPedidos(); }));
  paginacao.append(criarBotao('Próxima', pagina.last, () => { paginaAtual += 1; carregarPedidos(); }));
}

function criarBotao(texto, desabilitado, acao) {
  const botao = document.createElement('button');
  botao.className = 'button button-outline button-small';
  botao.textContent = texto;
  botao.disabled = desabilitado;
  botao.addEventListener('click', acao);
  return botao;
}