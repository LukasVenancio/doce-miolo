import { perfilAtual } from './auth.js';
import { moeda } from './formatacao.js';

export const CUPCAKE_IMAGE = 'images/cupcake-hero.jpg';

export function escapar(valor = '') {
  return String(valor).replace(/[&<>"']/g, caracter => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[caracter]);
}

export function toast(mensagem, tipo = 'success') {
  const regiao = document.querySelector('[data-toast-region]');
  if (!regiao) return;
  const elemento = document.createElement('div');
  elemento.className = 'toast';
  elemento.setAttribute('role', 'status');
  if (tipo === 'error') {
    elemento.classList.add('toast-error');
    elemento.setAttribute('role', 'alert');
  }
  elemento.textContent = mensagem;
  regiao.append(elemento);
  window.setTimeout(() => elemento.remove(), 4200);
}

export function mostrarAviso(elemento, mensagem, tipo = 'error') {
  if (!elemento) return;
  elemento.textContent = mensagem;
  elemento.className = `notice notice-${tipo}`;
  elemento.dataset.visible = 'true';
}

export function limparAviso(elemento) {
  if (!elemento) return;
  elemento.textContent = '';
  elemento.dataset.visible = 'false';
}

export function cardProduto(produto) {
  let acaoProduto = 'Ver detalhes';
  if (produto.personalizavel) acaoProduto = 'Personalizar';
  return `<article class="product-card">
    <a href="produto.html?id=${encodeURIComponent(produto.id)}" aria-label="Ver ${escapar(produto.nome)}">
      <img class="product-image" src="${escapar(produto.imagemUrl || CUPCAKE_IMAGE)}" alt="Cupcake ${escapar(produto.nome)}" loading="lazy">
    </a>
    <div class="product-card-body">
      <div class="product-meta"><span>${escapar(produto.categoriaNome || 'Feito artesanalmente')}</span><span>Gourmet</span></div>
      <h3>${escapar(produto.nome)}</h3>
      <p class="product-description">${escapar(produto.descricao)}</p>
      <div class="product-bottom">
        <span class="price">${moeda(produto.precoBase)}<small class="price-label">preço base</small></span>
        <a class="button button-small" href="produto.html?id=${encodeURIComponent(produto.id)}">${acaoProduto}</a>
      </div>
    </div>
  </article>`;
}

export function atualizarNavegacao() {
  const perfil = perfilAtual();
  document.querySelectorAll('[data-admin-link]').forEach(link => {
    link.classList.toggle('hidden', perfil !== 'ADMIN');
  });
  document.querySelectorAll('[data-session-link]').forEach(link => {
    link.textContent = 'Entrar';
    link.href = 'login.html';
    link.onclick = null;
    if (perfil) {
      link.textContent = 'Sair';
      link.href = '#';
      link.onclick = event => {
      event.preventDefault();
      import('./auth.js').then(auth => {
        auth.encerrarSessao();
        window.location.href = 'index.html';
      });
      };
    }
  });
}

export function menuMobile() {
  const botao = document.querySelector('[data-menu-toggle]');
  const links = document.querySelector('[data-nav-links]');
  if (!botao || !links) return;
  botao.addEventListener('click', () => {
    const aberto = links.dataset.open === 'true';
    links.dataset.open = String(!aberto);
    botao.setAttribute('aria-expanded', String(!aberto));
  });
}