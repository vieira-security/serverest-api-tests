package br.com.gabrielvieira.serverest.model;

/** Payload de /produtos. Preço e quantidade são numéricos para permitir testes de limite. */
public record Produto(String nome, Number preco, String descricao, Number quantidade) {
}
