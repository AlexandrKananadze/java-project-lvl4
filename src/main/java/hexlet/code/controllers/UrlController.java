package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import io.javalin.http.Handler;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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
                ctx.sessionAttribute("flash", "Страница успешно добавлена");
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

    public static Handler startCheck = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = new QUrl()
              .id.equalTo(id)
              .findOne();

        try {
            HttpResponse<String> response = Unirest
                    .get(url.getName())
                    .asString();

            UrlCheck check = createCheck(response, url);
            check.save();
        } catch (UnirestException e) {
            ctx.status(422);
            ctx.sessionAttribute("flash-type", "danger");
            ctx.sessionAttribute("flash", "Страница не отвечает!");
            ctx.redirect("/urls/" + id);
            return;
        }

        ctx.sessionAttribute("flash-type", "success");
        ctx.sessionAttribute("flash", "Страница успешно проверена");
        ctx.redirect("/urls/" + id);
    };

    private static UrlCheck createCheck(HttpResponse<String> resp, Url url) {
        String body = resp.getBody();
        Document document = Jsoup.parse(body);

        Element descriptionElem = document.selectFirst("meta[name=description]");
        String description = descriptionElem == null ? "" : descriptionElem.attr("content");

        Element h1Elem = document.selectFirst("h1");
        String h1 = h1Elem == null ? "" : h1Elem.text();

        return new UrlCheck(resp.getStatus(),
                            document.title(),
                            h1,
                            description,
                            url);
    }

    private static boolean checkIsUrlExists(String url) {
        return new QUrl()
                .name.equalTo(url)
                .exists();
    }
}
