import { describe, expect, test } from '@jest/globals';
import { criaPedidoRequest } from '../../main/resources/static/js/pedido.js';

describe('montagem do checkout', () => {
  test('envia endereço completo para entrega', () => {
    const dados = new FormData();
    dados.set('tipoEntrega', 'ENTREGA');
    dados.set('formaPagamento', 'PIX');
    dados.set('logradouro', 'Rua Central');
    dados.set('numero', '10');
    dados.set('complemento', '');
    dados.set('bairro', 'Centro');
    dados.set('cidade', 'São Paulo');
    dados.set('uf', 'sp');
    dados.set('cep', '01000-000');

    expect(criaPedidoRequest(dados)).toEqual({
      tipoEntrega: 'ENTREGA',
      formaPagamento: 'PIX',
      endereco: {
        logradouro: 'Rua Central',
        numero: '10',
        complemento: null,
        bairro: 'Centro',
        cidade: 'São Paulo',
        uf: 'SP',
        cep: '01000-000'
      }
    });
  });

  test('omite endereço na retirada', () => {
    const dados = new FormData();
    dados.set('tipoEntrega', 'RETIRADA');
    dados.set('formaPagamento', 'CARTAO');

    expect(criaPedidoRequest(dados)).toEqual({
      tipoEntrega: 'RETIRADA',
      endereco: null,
      formaPagamento: 'CARTAO'
    });
  });
});