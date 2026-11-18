import { describe, expect, test } from '@jest/globals';
import { moeda, validaCep, validaEmail } from '../../main/resources/static/js/formatacao.js';

describe('formatação e validação', () => {
  test('formata valores em reais', () => {
    expect(moeda(12.5)).toBe('R$ 12,50');
  });

  test('aceita email válido', () => {
    expect(validaEmail('ana@exemplo.com')).toBe(true);
    expect(validaEmail('ana@')).toBe(false);
  });

  test('aceita CEP com e sem hífen', () => {
    expect(validaCep('01000-000')).toBe(true);
    expect(validaCep('01000000')).toBe(true);
    expect(validaCep('1000-000')).toBe(false);
  });
});