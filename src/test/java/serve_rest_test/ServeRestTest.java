package serve_rest_test;

import com.fasterxml.jackson.databind.introspect.TypeResolutionContext;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServeRestTest {


    private static String userId;
    private static String token;

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "https://serverest.dev";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @Order(1)
    void shouldGetAllUsers(){
        given()
        .when()
            .get("/usuarios")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("quantidade", greaterThan(0))
                .body("usuarios", not(empty()));
    }

    @Test
    @Order(2)
    void shouldFindUserByEmail(){
        String emailOfFirstUser = given()
        .when()
                .get("/usuarios")
        .then()
                .extract()
                .path("usuarios[0].email");

        given()
            .contentType(ContentType.JSON)
            .param("email", emailOfFirstUser)
        .when()
            .get("/usuarios")
        .then()
            .statusCode(200)
            .body("usuarios", hasSize(1));
    }

    @Test
    @Order(3)
    void shouldCreateNewUser() {

        String newUser = "{\"nome\":\"Ann Grane\",\"email\":\"kj232d43ie8hgyge@qa.com\",\"password\":\"secret123\",\"administrador\":\"true\"}";

        Response response = given()
                .contentType(ContentType.JSON)
                .body(newUser)
        .when()
                .post("/usuarios")
        .then()
            .statusCode(201)
            .body("message", equalTo("Cadastro realizado com sucesso"))
            .body("_id", not(empty()))
            .extract()
            .response();

        userId = response.path("_id");
    }

    @Test
    @Order(4)

    void shouldUpdateUser(){

        String newUser = "{\"nome\":\"Обновленный покупатель\",\"email\":\"kj232d43ie8hgyge@qa.com\",\"password\":\"secret123\",\"administrador\":\"false\"}";

        given()
            .contentType(ContentType.JSON)
            .body(newUser)
            .pathParam("id", userId)
        .when()
            .put("/usuarios/{id}")
        .then()
            .statusCode(200)
            .body("message", equalTo("Registro alterado com sucesso"));
    }

    @Test
    @Order(5)
    void shouldLogin() {

        String login = "{\"email\":\"kj232d43ie8hgyge@qa.com\",\"password\":\"secret123\"}";

        Response response = given()
                .contentType(ContentType.JSON)
                .body(login)
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .body("message", equalTo("Login realizado com sucesso"))
                .body("authorization", not(empty()))
                .extract()
                .response();

        token = response.path("authorization");
    }

    @Test
    @Order(6)
    void shouldDeleteUser() {

        given()
            .header("Authorization", token)
            .pathParam("id", userId)
        .when()
            .delete("/usuarios/{id}")
        .then()
            .statusCode(200)
            .body("message", equalTo("Registro excluído com sucesso"));

        given()
             .pathParam("id", userId)
        .when()
             .get("/usuarios/{id}")
        .then()
             .statusCode(400)
             .body("message", equalTo("Usuário não encontrado"));
    }

    @Test
    @Order(7)
    void shouldGetAllProducts() {

        given()
        .when()
             .get("/produtos")
        .then()
            .statusCode(200)
            .body("quantidade", greaterThan(0))
            .body("produtos.preco", everyItem(greaterThan(0)))
            .body("produtos.nome", everyItem(not(empty())))
            .body("produtos.nome", hasItem("Awesome Plastic Gloves"));
    }
}
