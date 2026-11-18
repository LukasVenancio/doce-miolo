export function calculaPrecoUnitario(precoBase, adicionais = []) {
  return adicionais.reduce((total, adicional) => total + Number(adicional || 0), Number(precoBase) || 0);
}

export function calculaSubtotal(itens = []) {
  return itens.reduce((total, item) => total + Number(item.subtotal || 0), 0);
}

export function atualizaQuantidade(itens, itemId, quantidade) {
  return itens.map(item => {
    if (item.id !== itemId) return item;
    return { ...item, quantidade };
  });
}