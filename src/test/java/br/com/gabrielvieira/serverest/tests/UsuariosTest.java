package br.com.gabrielvieira.serverest.tests;

import br.com.gabrielvieira.serverest.base.BaseTest;
import br.com.gabrielvieira.serverest.factory.DataFactory;
import br.com.gabrielvieira.serverest.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

@DisplayName("/usuarios")
class UsuariosTest extends BaseTest {

    @Test
    @DisplayName("CT-U01 - Cadastrar usuário retorna 201 e o usuário fica disponível na busca por ID")
    void cadastrarUsuario() {
        Usuario usuario = DataFactory.usuarioComum();

        String id = given()
                .body(usuario)
            .when()
                .post("/usuarios")
            .then()
                .statusCode(201)
                .body("message", equalTo("Cadastro realizado com sucesso"))
                .body("_id", matchesPattern("^[A-Za-z0-9]{16}$"))
                .extract().path("_id");

        // Confirma a persistência, e não só a resposta do POST
        given()
                .pathParam("id", id)
            .when()
                .get("/usuarios/{id}")
            .then()
                .statusCode(200)
                .body("nome", equalTo(usuario.nome()))
                .body("email", equalTo(usuario.email()))
                .body("administrador", equalTo("false"))
                .body(matchesJsonSchemaInClasspath("schemas/usuario-schema.json"));
    }

    @Test
    @DisplayName("CT-U02 - Cadastrar usuário com e-mail já existente retorna 400")
    void cadastrarUsuarioComEmailDuplicado() {
        Usuario usuario = DataFactory.usuarioComum();
        cadastrarUsuario(usuario);

        given()
                .body(usuario)
            .when()
                .post("/usuarios")
            .then()
                .statusCode(400)
                .body("message", equalTo("Este email já está sendo usado"));
    }

    @Test
    @DisplayName("CT-U03 - Cadastrar usuário sem campos obrigatórios retorna 400 com mensagem por campo")
    void cadastrarUsuarioSemCamposObrigatorios() {
        given()
                .body("{}")
            .when()
                .post("/usuarios")
            .then()
                .statusCode(400)
                .body("nome", equalTo("nome é obrigatório"))
                .body("email", equalTo("email é obrigatório"))
                .body("password", equalTo("password é obrigatório"))
                .body("administrador", equalTo("administrador é obrigatório"));
    }

    @Test
    @DisplayName("CT-U04 - Listar usuários retorna contrato válido e o campo quantidade bate com a lista")
    void listarUsuarios() {
        var response = given()
            .when()
                .get("/usuarios")
            .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/lista-usuarios-schema.json"))
                .extract().response();

        int quantidade = response.path("quantidade");
        int tamanhoDaLista = response.path("usuarios.size()");
        org.junit.jupiter.api.Assertions.assertEquals(tamanhoDaLista, quantidade,
                "O campo 'quantidade' deve ser igual ao número de usuários retornados");
    }

    @Test
    @DisplayName("CT-U05 - Buscar usuário por e-mail (query param) retorna apenas o usuário filtrado")
    void buscarUsuarioPorEmail() {
        Usuario usuario = DataFactory.usuarioComum();
        cadastrarUsuario(usuario);

        given()
                .queryParam("email", usuario.email())
            .when()
                .get("/usuarios")
            .then()
                .statusCode(200)
                .body("quantidade", equalTo(1))
                .body("usuarios[0].email", equalTo(usuario.email()));
    }

    @Test
    @DisplayName("CT-U06 - Buscar usuário com ID inexistente retorna 400")
    void buscarUsuarioInexistente() {
        given()
                .pathParam("id", "AAAAAAAAAAAAAAAA")
            .when()
                .get("/usuarios/{id}")
            .then()
                .statusCode(400)
                .body("message", equalTo("Usuário não encontrado"));
    }

    @Test
    @DisplayName("CT-U07 - Buscar usuário com ID fora do formato (≠ 16 caracteres) retorna 400")
    void buscarUsuarioComIdInvalido() {
        given()
                .pathParam("id", "123")
            .when()
                .get("/usuarios/{id}")
            .then()
                .statusCode(400)
                .body("id", equalTo("id deve ter exatamente 16 caracteres alfanuméricos"));
    }

    @Test
    @DisplayName("CT-U08 - Editar usuário retorna 200 e a alteração é persistida")
    void editarUsuario() {
        Usuario original = DataFactory.usuarioComum();
        String id = cadastrarUsuario(original);
        Usuario editado = new Usuario("Nome Editado", original.email(), "novaSenha", "true");

        given()
                .pathParam("id", id)
                .body(editado)
            .when()
                .put("/usuarios/{id}")
            .then()
                .statusCode(200)
                .body("message", equalTo("Registro alterado com sucesso"));

        given()
                .pathParam("id", id)
            .when()
                .get("/usuarios/{id}")
            .then()
                .statusCode(200)
                .body("nome", equalTo("Nome Editado"))
                .body("administrador", equalTo("true"));
    }

    @Test
    @DisplayName("CT-U09 - Excluir usuário retorna 200 e ele deixa de existir")
    void excluirUsuario() {
        String id = cadastrarUsuario(DataFactory.usuarioComum());

        given()
                .pathParam("id", id)
            .when()
                .delete("/usuarios/{id}")
            .then()
                .statusCode(200)
                .body("message", equalTo("Registro excluído com sucesso"));

        given()
                .pathParam("id", id)
            .when()
                .get("/usuarios/{id}")
            .then()
                .statusCode(400)
                .body("message", equalTo("Usuário não encontrado"));
    }

    @Test
    @DisplayName("CT-U10 - Excluir usuário inexistente retorna 200 sem excluir nada")
    void excluirUsuarioInexistente() {
        given()
                .pathParam("id", "AAAAAAAAAAAAAAAA")
            .when()
                .delete("/usuarios/{id}")
            .then()
                .statusCode(200)
                .body("message", equalTo("Nenhum registro excluído"));
    }
}
