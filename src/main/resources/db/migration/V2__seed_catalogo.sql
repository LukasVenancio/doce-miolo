INSERT INTO categoria (nome) VALUES
    ('Clássicos'),
    ('Frutados'),
    ('Especiais');

INSERT INTO opcao_personalizacao (tipo, nome, preco_adicional) VALUES
    ('MASSA', 'Baunilha', 0.00),
    ('MASSA', 'Chocolate', 1.00),
    ('MASSA', 'Red velvet', 2.00),
    ('RECHEIO', 'Brigadeiro', 0.00),
    ('RECHEIO', 'Doce de leite', 1.50),
    ('RECHEIO', 'Creme de avelã', 2.50),
    ('COBERTURA', 'Ganache', 0.00),
    ('COBERTURA', 'Buttercream', 1.50),
    ('COBERTURA', 'Cream cheese', 2.00),
    ('EXTRA', 'Granulado', 0.50),
    ('EXTRA', 'Morango', 1.50),
    ('EXTRA', 'Castanha caramelizada', 2.00);

INSERT INTO produto (categoria_id, nome, descricao, preco_base, imagem_url, personalizavel, ativo)
SELECT id, 'Brigadeiro Gourmet', 'Massa de chocolate com cobertura de ganache.', 12.00, NULL, TRUE, TRUE
FROM categoria WHERE nome = 'Clássicos';

INSERT INTO produto (categoria_id, nome, descricao, preco_base, imagem_url, personalizavel, ativo)
SELECT id, 'Red Velvet', 'Massa red velvet com cobertura cremosa.', 14.00, NULL, TRUE, TRUE
FROM categoria WHERE nome = 'Especiais';

INSERT INTO produto (categoria_id, nome, descricao, preco_base, imagem_url, personalizavel, ativo)
SELECT id, 'Morango com Baunilha', 'Massa leve de baunilha com toque de morango.', 13.00, NULL, TRUE, TRUE
FROM categoria WHERE nome = 'Frutados';