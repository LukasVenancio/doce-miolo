const TOKEN_KEY = 'cupcakes.accessToken';

export function tokenAtual() {
  return localStorage.getItem(TOKEN_KEY);
}

export function salvarToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function encerrarSessao() {
  localStorage.removeItem(TOKEN_KEY);
}

export function perfilAtual() {
  const token = tokenAtual();
  if (!token) return null;
  const payload = token.split('.')[1];
  if (!payload) return null;
  const normalizado = payload.replace(/-/g, '+').replace(/_/g, '/');
  const conteudo = JSON.parse(decodeURIComponent(escape(atob(normalizado))));
  return conteudo.perfil || null;
}

export function requerPerfil(perfil) {
  return perfilAtual() === perfil;
}