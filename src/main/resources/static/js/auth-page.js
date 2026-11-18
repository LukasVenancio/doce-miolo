import { post } from './api.js';
import { salvarToken } from './auth.js';
import { validaEmail } from './formatacao.js';
import { menuMobile, mostrarAviso } from './ui.js';

menuMobile();

const loginForm = document.querySelector('[data-login-form]');
const cadastroForm = document.querySelector('[data-register-form]');

if (loginForm) loginForm.addEventListener('submit', entrar);
if (cadastroForm) cadastroForm.addEventListener('submit', cadastrar);

function entrar(event) {
  event.preventDefault();
  const dados = Object.fromEntries(new FormData(loginForm));
  const aviso = loginForm.querySelector('[data-notice]');
  if (!validaEmail(dados.email)) {
    mostrarAviso(aviso, 'Informe um endereço de e-mail válido.');
    return;
  }
  post('/api/auth/login', dados).then(token => {
    salvarToken(token.accessToken);
    const retorno = new URLSearchParams(window.location.search).get('retorno');
    window.location.href = urlRetornoValida(retorno);
  }, erro => mostrarAviso(aviso, erro.message));
}

function urlRetornoValida(retorno) {
  if (!retorno) return 'index.html';
  const destino = new URL(retorno, window.location.origin);
  if (destino.origin !== window.location.origin) return 'index.html';
  return `${destino.pathname}${destino.search}${destino.hash}`;
}

function cadastrar(event) {
  event.preventDefault();
  const dados = Object.fromEntries(new FormData(cadastroForm));
  const aviso = cadastroForm.querySelector('[data-notice]');
  if (!validaEmail(dados.email)) {
    mostrarAviso(aviso, 'Informe um endereço de e-mail válido.');
    return;
  }
  if (dados.senha.length < 8) {
    mostrarAviso(aviso, 'A senha precisa ter pelo menos 8 caracteres.');
    return;
  }
  post('/api/auth/cadastro', dados).then(() => {
    mostrarAviso(aviso, 'Conta criada. Entre para começar seu pedido.', 'success');
    cadastroForm.reset();
    window.setTimeout(() => { window.location.href = 'login.html'; }, 1000);
  }, erro => mostrarAviso(aviso, erro.message));
}