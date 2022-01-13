package hexlet.code;

import hexlet.code.controllers.RootController;
import hexlet.code.controllers.UrlController;
import io.javalin.Javalin;
import io.javalin.plugin.rendering.template.JavalinThymeleaf;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

public class App {
    public static void main(String[] args) {
        Javalin app = getApp();
        app.start(getPort());
    }

    public static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "4000");
        return Integer.parseInt(port);
    }

    private static void getRoutes(Javalin javalin) {
        javalin.get("/", RootController.main);

        javalin.routes(() -> {
            path("urls", () -> {
                post(UrlController.createUrl);
                get(UrlController.showUrls);
                path("/{id}", () -> {
                    get(UrlController.showUrl);
                    post("/checks", UrlController.startCheck);
                });
            });
        });
    }

    public static Javalin getApp() {
        Javalin app = Javalin.create(javalinConfig -> {
            if (!isProduction()) {
                javalinConfig.enableDevLogging();
            }
            javalinConfig.enableWebjars();
            JavalinThymeleaf.configure(getTemplateEngine());
        });

        app.before(ctx -> {
            ctx.attribute("ctx", ctx);
        });

        getRoutes(app);

        return app;
    }

    private static boolean isProduction() {
        return getMode().equals("production");
    }

    private static String getMode() {
        return System.getenv().getOrDefault("APP_ENV", "development");
    }

    private static TemplateEngine getTemplateEngine() {
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();

        templateResolver.setPrefix("/templates/");
        templateEngine.addTemplateResolver(templateResolver);
        templateEngine.addDialect(new LayoutDialect());
        templateEngine.addDialect(new Java8TimeDialect());

        return templateEngine;
    }
}
