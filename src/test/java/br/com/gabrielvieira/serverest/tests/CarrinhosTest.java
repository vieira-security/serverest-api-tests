package br.com.gabrielvieira.serverest.tests;

import br.com.gabrielvieira.serverest.base.BaseTest;
import br.com.gabrielvieira.serverest.factory.DataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Fluxo de compra (testes de integração entre /carrinhos e /produtos).
 *
 * Cada teste usa um administrador e um produto novos (estoque = 5),
 * porque a API permite só 1 carrinho por usuário e o estoque muda a cada compra.
 */
@DisplayName("/carrinhos")
class CarrinhosTest extends BaseTest {

    private static final int ESTOQUE_INICIAL = 5;

    private String token;
    private String idProduto;

    @BeforeEach
    void prepararUsuarioEProduto() {
        token = tokenAdmin();
        idProduto = cadastrarProduto(DataFactory.produto(100, ESTOQUE_INICIAL), token);
    }

    private String carrinhoCom(int quantidade) {
        return """
                {"produtos": [{"idProduto": "%s", "quantidade": %d}]}
                """.formatted(idProduto, quantidade);
    }

    private void criarCarrinho(int quantidade) {
        given()
                .header("Authorization", token)
                .body(carrinhoCom(quantidade))
            .when()
                .post("/carrinhos")
            .then()
                .statusCode(201);
    }

    private int estoqueAtual() {
        return given().pathParam("id", idProduto).get("/produtos/{id}").path("quantidade");
    }

    @Test
    @DisplayName("CT-C01 - Criar carrinho retorna 201 e baixa o estoque do produto")
    void criarCarrinhoBaixaEstoque() {
        given()
                .header("Authorization", token)
                .body(carrinhoCom(2))
            .when()
                .post("/carrinhos")
            .then()
                .statusCode(201)
                .body("message", equalTo("Cadastro realizado com sucesso"));

        org.junit.jupiter.api.Assertions.assertEquals(ESTOQUE_INICIAL - 2, estoqueAtual());
    }

    @Test
    @DisplayName("CT-C02 - Criar um segundo carrinho para o mesmo usuário retorna 400")
    void naoPermiteDoisCarrinhos() {
        criarCarrinho(1);

        given()
                .header("Authorization", token)
                .body(carrinhoCom(1))
            .when()
                .post("/carrinhos")
            .then()
                .statusCode(400)
                .body("message", equalTo("Não é permitido ter mais de 1 carrinho"));
    }

    @Test
    @DisplayName("CT-C03 - Carrinho com quantidade acima do estoque retorna 400 e não altera o estoque")
    void quantidadeAcimaDoEstoque() {
        given()
                .header("Authorization", token)
                .body(carrinhoCom(ESTOQUE_INICIAL + 1))
            .when()
                .post("/carrinhos")
            .then()
                .statusCode(400)
                .body("message", equalTo("Produto não possui quantidade suficiente"))
                .body("item.quantidadeEstoque", equalTo(ESTOQUE_INICIAL));

        org.junit.jupiter.api.Assertions.assertEquals(ESTOQUE_INICIAL, estoqueAtual());
    }

    @Test
    @DisplayName("CT-C04 - Excluir produto que está em um carrinho retorna 400")
    void naoPermiteExcluirProdutoEmCarrinho() {
        criarCarrinho(1);

        given()
                .header("Authorization", token)
                .pathParam("id", idProduto)
            .when()
                .delete("/produtos/{id}")
            .then()
                .statusCode(400)
                .body("message", equalTo("Não é permitido excluir produto que faz parte de carrinho"))
                .body("idCarrinhos", hasSize(1));
    }

    @Test
    @DisplayName("CT-C05 - Cancelar compra exclui o carrinho e devolve o estoque")
    void cancelarCompraReabasteceEstoque() {
        criarCarrinho(3);

        given()
                .header("Authorization", token)
            .when()
                .delete("/carrinhos/cancelar-compra")
            .then()
                .statusCode(200)
                .body("message", equalTo("Registro excluído com sucesso. Estoque dos produtos reabastecido"));

        org.junit.jupiter.api.Assertions.assertEquals(ESTOQUE_INICIAL, estoqueAtual());
    }

    @Test
    @DisplayName("CT-C06 - Concluir compra exclui o carrinho e mantém o estoque baixado")
    void concluirCompra() {
        criarCarrinho(3);

        given()
                .header("Authorization", token)
            .when()
                .delete("/carrinhos/concluir-compra")
            .then()
                .statusCode(200)
                .body("message", equalTo("Registro excluído com sucesso"));

        org.junit.jupiter.api.Assertions.assertEquals(ESTOQUE_INICIAL - 3, estoqueAtual());
    }

    @Test
    @DisplayName("CT-C07 - Concluir compra sem carrinho retorna mensagem de carrinho não encontrado")
    void concluirCompraSemCarrinho() {
        given()
                .header("Authorization", token)
            .when()
                .delete("/carrinhos/concluir-compra")
            .then()
                .statusCode(200)
                .body("message", equalTo("Não foi encontrado carrinho para esse usuário"));
    }
}
