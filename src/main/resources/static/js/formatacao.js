export function moeda(valor) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number(valor) || 0);
}

export function dataHora(valor) {
  if (!valor) return 'Data indisponível';
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(valor));
}

export function nomeStatus(status) {
  const nomes = {
    AGUARDANDO_PAGAMENTO: 'Aguardando pagamento',
    PAGO: 'Pago',
    EM_PREPARO: 'Em preparo',
    SAIU_PARA_ENTREGA: 'Saiu para entrega',
    ENTREGUE: 'Entregue',
    CANCELADO: 'Cancelado',
    PENDENTE: 'Pendente',
    CANCELADO_PAGAMENTO: 'Cancelado'
  };
  return nomes[status] || status;
}

export function validaEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(email).trim());
}

export function validaCep(cep) {
  return /^\d{5}-?\d{3}$/.test(String(cep).trim());
}