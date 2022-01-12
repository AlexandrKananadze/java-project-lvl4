package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import io.javalin.http.Handler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class UrlController {
    public static Handler createUrl = ctx -> {
        String urlFromUser = ctx.formParam("url");

        try {
            URL url = new URL(urlFromUser);
            String editedUrl = url.getProtocol() + "://" + url.getAuthority();
            if (!checkIsUrlExists(editedUrl)) {
                new Url(editedUrl).save();
                ctx.sessionAttribute("flash-type", "success");
                ctx.sessionAttribute("flash", "Страница добавлена!");
                ctx.redirect("/urls");
                return;
            }

            ctx.sessionAttribute("flash-type", "danger");
            ctx.sessionAttribute("flash", "Страница уже существует.");
        } catch (MalformedURLException e) {
            ctx.sessionAttribute("flash-type", "danger");
            ctx.sessionAttribute("flash", "Некорректный URL");
        }

        ctx.sessionAttribute("flash-type", "danger");
        ctx.status(422);
        ctx.render("index.html");
    };

    public static Handler showUrls = ctx -> {
        int currentPage = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        final int rowsForPage = 10;
        final int offset = (currentPage - 1) * rowsForPage;

        List<Url> urls = new QUrl()
                .setFirstRow(offset)
                .setMaxRows(rowsForPage)
                .orderBy().id.asc()
                .findList();

        Map<Long, UrlCheck> urlChecks = new QUrlCheck()
                .url.id.asMapKey()
                .orderBy()
                .createdAt.desc()
                .findMap();

        ctx.attribute("page", currentPage);
        ctx.attribute("checks", urlChecks);
        ctx.attribute("urls", urls);
        ctx.render("urls/index.html");
    };

    public static Handler showUrl = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (url == null) {
            ctx.sessionAttribute("flash-type", "danger");
            ctx.sessionAttribute("flash", "Некорректный ID.");
            ctx.render("/urls");
            return;
        }

        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };

    private static boolean checkIsUrlExists(String url) {
        return new QUrl()
                .name.equalTo(url)
                .exists();
    }
}
