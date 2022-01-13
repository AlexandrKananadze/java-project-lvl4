package hexlet.code;

import hexlet.code.domain.Url;
import io.ebean.DB;
import io.ebean.Transaction;
import io.javalin.Javalin;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AppTest {
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
}
