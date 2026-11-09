package br.com.gabrielvieira.serverest.factory;

import br.com.gabrielvieira.serverest.model.Produto;
import br.com.gabrielvieira.serverest.model.Usuario;

import java.util.UUID;

/**
 * Gera massa de dados única a cada chamada.
 *
 * E-mails e nomes de produto são únicos na ServeRest, então cada teste cria
 * os próprios dados. Assim os testes não dependem uns dos outros e podem
 * rodar em qualquer ordem.
 */
public final class DataFactory {

    private DataFactory() {
    }

    private static String sufixo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static Usuario usuarioAdmin() {
        String s = sufixo();
        return new Usuario("QA Admin " + s, "admin_" + s + "@qa.com", "senha123", "true");
    }

    public static Usuario usuarioComum() {
        String s = sufixo();
        return new Usuario("QA Comum " + s, "comum_" + s + "@qa.com", "senha123", "false");
    }

    public static Produto produto() {
        return produto(100, 10);
    }

    public static Produto produto(Number preco, Number quantidade) {
        return new Produto("Produto QA " + sufixo(), preco, "Produto criado pela suíte de testes", quantidade);
    }
}
