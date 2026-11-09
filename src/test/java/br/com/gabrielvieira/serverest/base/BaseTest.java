package br.com.gabrielvieira.serverest.base;

import br.com.gabrielvieira.serverest.factory.DataFactory;
import br.com.gabrielvieira.serverest.model.Login;
import br.com.gabrielvieira.serverest.model.Produto;
import br.com.gabrielvieira.serverest.model.Usuario;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;

import static io.restassured.RestAssured.given;

/**
 * Configuração comum a todas as classes de teste.
 *
 * A URL da API vem da propriedade -DbaseUrl ou da variável de ambiente BASE_URL.
 * Por padrão aponta para a ServeRest rodando localmente (npx serverest),
 * o que deixa a suíte independente da instância pública serverest.dev.
 */
public abstract class BaseTest {

    @BeforeAll
    static void configurarRestAssured() {
        String baseUrl = System.getProperty("baseUrl",
                System.getenv().getOrDefault("BASE_URL", "http://localhost:3000"));

        RestAssured.baseURI = baseUrl;
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
        // Só imprime request/response no console quando uma validação falha
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // ---------- Helpers de pré-condição (setup via API) ----------

    /** Cadastra o usuário e devolve o _id gerado. */
    protected static String cadastrarUsuario(Usuario usuario) {
        return given()
                .body(usuario)
            .when()
                .post("/usuarios")
            .then()
                .statusCode(201)
                .extract().path("_id");
    }

    /** Faz login e devolve o token no formato "Bearer ...". */
    protected static String obterToken(Usuario usuario) {
        return given()
                .body(new Login(usuario.email(), usuario.password()))
            .when()
                .post("/login")
            .then()
                .statusCode(200)
                .extract().path("authorization");
    }

    /** Cria um usuário administrador novo e devolve o token dele. */
    protected static String tokenAdmin() {
        Usuario admin = DataFactory.usuarioAdmin();
        cadastrarUsuario(admin);
        return obterToken(admin);
    }

    /** Cadastra um produto com o token informado e devolve o _id. */
    protected static String cadastrarProduto(Produto produto, String token) {
        return given()
                .header("Authorization", token)
                .body(produto)
            .when()
                .post("/produtos")
            .then()
                .statusCode(201)
                .extract().path("_id");
    }
}
