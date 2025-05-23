package neoflix;

import java.util.*;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import neoflix.routes.*;

import static io.javalin.apibuilder.ApiBuilder.path;

public class NeoflixApp {

    public static void main(String[] args) {
        AppUtils.loadProperties();

        // tag::driver[]
        var driver = AppUtils.initDriver();
        // end::driver[]

        var jwtSecret = AppUtils.getJwtSecret();
        var port = AppUtils.getServerPort();

        var gson = GsonUtils.gson();
        // Initialize the Javalin server with API endpoints
        var server = Javalin
            .create(config -> {
                config.staticFiles.add(staticFiles -> {
                    staticFiles.directory = "/public";
                    staticFiles.location = Location.CLASSPATH;
                });
                config.router.apiBuilder(() -> {
                    path("api", () -> {
                        path("movies", () -> new MovieRoutes(driver, gson).addEndpoints());
                        path("genres", () -> new GenreRoutes(driver, gson).addEndpoints());
                        path("auth", () -> new AuthRoutes(driver, gson, jwtSecret).addEndpoints());
                        path("account", () -> new AccountRoutes(driver, gson).addEndpoints());
                        path("people", () -> new PeopleRoutes(driver, gson).addEndpoints());
                    });
                });
            })
            .before(ctx -> AppUtils.handleAuthAndSetUser(ctx.req(), jwtSecret))
            .exception(ValidationException.class, (exception, ctx) -> {
                var body = Map.of("message", exception.getMessage(), "details", exception.getDetails());
                ctx.status(422).contentType("application/json").result(gson.toJson(body));
            });

        server.start(port);
        System.out.printf("Server listening on http://localhost:%d/%n", port);
    }
}