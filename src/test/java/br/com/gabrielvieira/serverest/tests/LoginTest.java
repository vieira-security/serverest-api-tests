package br.com.gabrielvieira.serverest.tests;

import br.com.gabrielvieira.serverest.base.BaseTest;
import br.com.gabrielvieira.serverest.factory.DataFactory;
import br.com.gabrielvieira.serverest.model.Login;
import br.com.gabrielvieira.serverest.model.Usuario;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

@DisplayName("POST /login")
class LoginTest extends BaseTest {

    private static Usuario usuario;

    @BeforeAll
    static void criarUsuario() {
        usuario = DataFactory.usuarioComum();
        cadastrarUsuario(usuario);
    }

    @Test
    @DisplayName("CT-L01 - Login com credenciais válidas retorna 200 e token Bearer")
    void loginComCredenciaisValidas() {
        given()
                .body(new Login(usuario.email(), usuario.password()))
            .when()
                .post("/login")
            .then()
                .statusCode(200)
                .body("message", equalTo("Login realizado com sucesso"))
                .body("authorization", startsWith("Bearer "))
                .body(matchesJsonSchemaInClasspath("schemas/login-schema.json"));
    }

    // Partição de equivalência: cada linha representa uma classe inválida de credencial
    @ParameterizedTest(name = "CT-L02 - Credencial inválida: {0}")
    @CsvSource({
            "senha incorreta,        CORRETO,             senhaErrada",
            "e-mail não cadastrado,  naoexiste@qa.com,    senha123",
            "senha com maiúsculas,   CORRETO,             SENHA123"
    })
    void loginComCredenciaisInvalidas(String cenario, String email, String senha) {
        String emailUsado = email.equals("CORRETO") ? usuario.email() : email;

        given()
                .body(new Login(emailUsado, senha))
            .when()
                .post("/login")
            .then()
                .statusCode(401)
                .body("message", equalTo("Email e/ou senha inválidos"))
                .body("$", not(hasKey("authorization")));
    }

    @Test
    @DisplayName("CT-L03 - Login com campos em branco retorna 400 com mensagem por campo")
    void loginComCamposEmBranco() {
        given()
                .body(new Login("", ""))
            .when()
                .post("/login")
            .then()
                .statusCode(400)
                .body("email", equalTo("email não pode ficar em branco"))
                .body("password", equalTo("password não pode ficar em branco"));
    }

    @Test
    @DisplayName("CT-L04 - Login com e-mail em formato inválido retorna 400")
    void loginComEmailInvalido() {
        given()
                .body(new Login("email-sem-arroba", "senha123"))
            .when()
                .post("/login")
            .then()
                .statusCode(400)
                .body("email", equalTo("email deve ser um email válido"));
    }
}
