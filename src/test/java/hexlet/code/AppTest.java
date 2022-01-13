package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.ebean.DB;
import io.ebean.Transaction;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
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

    @Test
    void testCreateUrl() {
        HttpResponse postResponse = Unirest
                .post(baseUrl + "/urls")
                .field("url", "https://newsite.com")
                .asEmpty();

        HttpResponse<String> getResponse = Unirest
                .get(baseUrl + "/urls")
                .asString();

        String body = getResponse.getBody();

        Url url = new QUrl()
                .name.equalTo("https://newsite.com")
                .findOne();

        assertThat(getResponse.getStatus()).isEqualTo(200);
        assertThat(url).isNotNull();
        assertThat(body).contains("Страница добавлена!");
        assertThat(body).contains("https://newsite.com");
    }

    @Test
    void testShowUrl() {
        HttpResponse<String> resp = Unirest
                .get(baseUrl + "/urls/" + url.getId())
                .asString();

        String respBody = resp.getBody();

        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(respBody).contains(url.getName());
    }

    @Test
    void testExistingUrl() {
        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls")
                .field("url", "https://somemocksite.com")
                .asString();

        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(422);
        assertThat(body).contains("Страница уже существует.");
    }

    @Test
    void testInvalidUrl() {
        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls")
                .field("url", "notexists.com")
                .asString();

        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(422);
        assertThat(body).contains("Некорректный URL");
    }

    @Test
    void testExistentUrl() {
        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls")
                .field("url", "https://somemocksite.com")
                .asString();

        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(422);
        assertThat(body).contains("Страница уже существует.");
    }

    @Test
    void testMockParse() throws IOException {
        mockWebServer.enqueue(new MockResponse().setBody(html));
        mockWebServer.start();

        String mockUrl = mockWebServer.url("/").toString();

        HttpResponse postResp = Unirest
                .post(baseUrl + "/urls")
                .field("url", mockUrl)
                .asEmpty();

        Url url = new QUrl()
                .name.equalTo(mockUrl.replaceAll("/$", ""))
                .findOne();

        HttpResponse resp = Unirest
                .post(baseUrl + "/urls/" + url.getId() + "/checks")
                .asEmpty();

        HttpResponse<String> getResp = Unirest
                .get(baseUrl + "/urls/" + url.getId())
                .asString();

        String body = getResp.getBody();

        mockWebServer.shutdown();

        assertThat(getResp.getStatus()).isEqualTo(200);
        assertThat(body).contains("descriptionExpected");
        assertThat(body).contains("titleExpected");
        assertThat(body).contains("h1Expected");
    }
}
