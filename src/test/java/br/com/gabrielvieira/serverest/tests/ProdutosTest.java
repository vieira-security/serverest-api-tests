package br.com.gabrielvieira.serverest.tests;

import br.com.gabrielvieira.serverest.base.BaseTest;
import br.com.gabrielvieira.serverest.factory.DataFactory;
import br.com.gabrielvieira.serverest.model.Produto;
import br.com.gabrielvieira.serverest.model.Usuario;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("/produtos")
class ProdutosTest extends BaseTest {

    private static String tokenAdmin;

    @BeforeAll
    static void autenticarAdmin() {
        tokenAdmin = tokenAdmin();
    }

    // ---------- Autenticação e autorização ----------

    @Test
    @DisplayName("CT-P01 - Cadastrar produto sem token retorna 401")
    void cadastrarProdutoSemToken() {
        given()
                .body(DataFactory.produto())
            .when()
                .post("/produtos")
            .then()
                .statusCode(401)
                .body("message", equalTo("Token de acesso ausente, inválido, expirado ou usuário do token não existe mais"));
    }

    @Test
    @DisplayName("CT-P02 - Cadastrar produto com token adulterado retorna 401")
    void cadastrarProdutoComTokenInvalido() {
        given()
                .header("Authorization", tokenAdmin + "adulterado")
                .body(DataFactory.produto())
            .when()
                .post("/produtos")
            .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("CT-P03 - Cadastrar produto com usuário não administrador retorna 403")
    void cadastrarProdutoComUsuarioComum() {
        Usuario comum = DataFactory.usuarioComum();
        cadastrarUsuario(comum);

        given()
                .header("Authorization", obterToken(comum))
                .body(DataFactory.produto())
            .when()
                .post("/produtos")
            .then()
                .statusCode(403)
                .body("message", equalTo("Rota exclusiva para administradores"));
    }

    // ---------- Regras de negócio ----------

    @Test
    @DisplayName("CT-P04 - Cadastrar produto com administrador retorna 201 e o produto é persistido")
    void cadastrarProdutoComAdmin() {
        Produto produto = DataFactory.produto(250, 7);

        String id = given()
                .header("Authorization", tokenAdmin)
                .body(produto)
            .when()
                .post("/produtos")
            .then()
                .statusCode(201)
                .body("message", equalTo("Cadastro realizado com sucesso"))
                .extract().path("_id");

        given()
                .pathParam("id", id)
            .when()
                .get("/produtos/{id}")
            .then()
                .statusCode(200)
                .body("nome", equalTo(produto.nome()))
                .body("preco", equalTo(250))
                .body("quantidade", equalTo(7));
    }

    @Test
    @DisplayName("CT-P05 - Cadastrar produto com nome já existente retorna 400")
    void cadastrarProdutoDuplicado() {
        Produto produto = DataFactory.produto();
        cadastrarProduto(produto, tokenAdmin);

        given()
                .header("Authorization", tokenAdmin)
                .body(produto)
            .when()
                .post("/produtos")
            .then()
                .statusCode(400)
                .body("message", equalTo("Já existe produto com esse nome"));
    }

    // ---------- Análise de valor-limite (BVA) ----------
    // Regra da API: preço deve ser inteiro positivo (mínimo 1) e quantidade >= 0.
    // Testamos exatamente em cima e logo abaixo de cada limite.

    @ParameterizedTest(name = "CT-P06 - Limite válido: preço={0}, quantidade={1}")
    @CsvSource({
            "1, 0",   // menor preço válido + menor quantidade válida
            "1, 1",
            "2, 0"
    })
    void cadastrarProdutoNosLimitesValidos(int preco, int quantidade) {
        given()
                .header("Authorization", tokenAdmin)
                .body(DataFactory.produto(preco, quantidade))
            .when()
                .post("/produtos")
            .then()
                .statusCode(201);
    }

    @Test
    @DisplayName("CT-P07 - Limite inválido: preço 0 retorna 400")
    void cadastrarProdutoComPrecoZero() {
        given()
                .header("Authorization", tokenAdmin)
                .body(DataFactory.produto(0, 1))
            .when()
                .post("/produtos")
            .then()
                .statusCode(400)
                .body("preco", equalTo("preco deve ser um número positivo"));
    }

    @Test
    @DisplayName("CT-P08 - Limite inválido: quantidade -1 retorna 400")
    void cadastrarProdutoComQuantidadeNegativa() {
        given()
                .header("Authorization", tokenAdmin)
                .body(DataFactory.produto(1, -1))
            .when()
                .post("/produtos")
            .then()
                .statusCode(400)
                .body("quantidade", equalTo("quantidade deve ser maior ou igual a 0"));
    }

    @Test
    @DisplayName("CT-P09 - Preço decimal retorna 400 (a API só aceita inteiros)")
    void cadastrarProdutoComPrecoDecimal() {
        given()
                .header("Authorization", tokenAdmin)
                .body(DataFactory.produto(1.5, 1))
            .when()
                .post("/produtos")
            .then()
                .statusCode(400)
                .body("preco", equalTo("preco deve ser um inteiro"));
    }

    @Test
    @DisplayName("CT-P10 - Excluir produto com administrador retorna 200")
    void excluirProduto() {
        String id = cadastrarProduto(DataFactory.produto(), tokenAdmin);

        given()
                .header("Authorization", tokenAdmin)
                .pathParam("id", id)
            .when()
                .delete("/produtos/{id}")
            .then()
                .statusCode(200)
                .body("message", equalTo("Registro excluído com sucesso"));
    }
}
