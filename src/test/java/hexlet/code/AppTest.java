package hexlet.code;

import hexlet.code.domain.Url;
import io.ebean.DB;
import io.ebean.Transaction;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {
    private static Javalin app;
    private static String baseUrl;
    private static Transaction transaction;
    private static Url url;
    private static MockWebServer mockWebServer;
    private static String html;

    @BeforeAll
    public static void init() throws IOException {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        url = new Url("https://somemocksite.com");
        url.save();
        html = Files.readString(Path.of("src/test/resources/fixtures/htmlFixture.html"));
        mockWebServer = new MockWebServer();
    }

    @AfterAll
    public static void destroy() {
        app.stop();
    }

    @BeforeEach
    void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    void afterEach() throws IOException {
        transaction.rollback();
        mockWebServer.shutdown();
    }

    @Test
    void testMainPage() {
        HttpResponse<String> response = Unirest
                .get(baseUrl)
                .asString();

        String responseBody = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(responseBody).contains("Анализатор страниц");
    }

    @Test
    void testUrls() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();

        String responseBody = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(responseBody).contains("Сайты");
        assertThat(responseBody).contains("Последняя проверка");
        assertThat(responseBody).contains("Код ответа");
    }
}
