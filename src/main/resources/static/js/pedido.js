export function criaPedidoRequest(formData) {
  const tipoEntrega = formData.get('tipoEntrega');
  const enderecoCompleto = tipoEntrega === 'ENTREGA';
  let endereco = null;
  if (enderecoCompleto) {
    endereco = {
      logradouro: formData.get('logradouro').trim(),
      numero: formData.get('numero').trim(),
      complemento: formData.get('complemento').trim() || null,
      bairro: formData.get('bairro').trim(),
      cidade: formData.get('cidade').trim(),
      uf: formData.get('uf').trim().toUpperCase(),
      cep: formData.get('cep').trim()
    };
  }
  return { tipoEntrega, endereco, formaPagamento: formData.get('formaPagamento') };
}