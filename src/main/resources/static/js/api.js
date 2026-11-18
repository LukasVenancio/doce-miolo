import { encerrarSessao, tokenAtual } from './auth.js';

export class ApiError extends Error {
  constructor(message, status, body) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

export async function requisicao(path, options = {}) {
  const headers = new Headers(options.headers || {});
  const token = tokenAtual();
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  const response = await fetch(path, { ...options, headers });
  if (response.status === 401) encerrarSessao();
  if (response.status === 204) return null;
  const body = await response.json().then(conteudo => conteudo, () => null);
  if (!response.ok) throw new ApiError(body?.detail || 'Não foi possível concluir a solicitação.', response.status, body);
  return body;
}

export function get(path) {
  return requisicao(path);
}

export function post(path, body) {
  return requisicao(path, { method: 'POST', body: JSON.stringify(body) });
}

export function put(path, body) {
  return requisicao(path, { method: 'PUT', body: JSON.stringify(body) });
}

export function patch(path, body) {
  return requisicao(path, { method: 'PATCH', body: JSON.stringify(body) });
}

export function del(path) {
  return requisicao(path, { method: 'DELETE' });
}

export function queryString(params) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, value);
  });
  const resultado = query.toString();
  if (!resultado) return '';
  return `?${resultado}`;
}