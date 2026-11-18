import { describe, expect, test } from '@jest/globals';
import { calculaPrecoUnitario, calculaSubtotal, atualizaQuantidade } from '../../main/resources/static/js/carrinho.js';

describe('cálculos do carrinho', () => {
  test('soma preço base e adicionais', () => {
    expect(calculaPrecoUnitario(10, [1.5, 2])).toBe(13.5);
  });

  test('calcula o subtotal total dos itens', () => {
    expect(calculaSubtotal([{ subtotal: 12 }, { subtotal: 7.5 }])).toBe(19.5);
  });

  test('atualiza a quantidade sem alterar os outros itens', () => {
    const resultado = atualizaQuantidade([{ id: 1, quantidade: 1 }, { id: 2, quantidade: 2 }], 2, 4);
    expect(resultado).toEqual([{ id: 1, quantidade: 1 }, { id: 2, quantidade: 4 }]);
  });
});