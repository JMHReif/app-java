package example;
// tag::import[]
// Import all relevant classes from neo4j-java-driver dependency
import neoflix.AppUtils;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import org.neo4j.driver.async.AsyncSession;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.neo4j.driver.reactivestreams.ReactiveResult;
import org.neo4j.driver.reactivestreams.ReactiveSession;
// end::import[]

public class AsyncApi {

    static {
// Load config from .env
        AppUtils.loadProperties();
    }

    // Load Driver
    static Driver driver = GraphDatabase.driver(System.getProperty("NEO4J_URI"),
            AuthTokens.basic(System.getProperty("NEO4J_USERNAME"), System.getProperty("NEO4J_PASSWORD")));

    static void syncExample() {
        // tag::sync[]
        try (var session = driver.session()) {

            var res = session.executeRead(tx -> tx.run(
                    "MATCH (p:Person) RETURN p.name AS name LIMIT 10").list());
            res.stream()
                    .map(row -> row.get("name"))
                    .forEach(System.out::println);
        } catch (Exception e) {
            // There was a problem with the
            // database connection or the query
            e.printStackTrace();
        }
        // end::sync[]
    }

    static void asyncExample() {
        // tag::async[]
        var session = driver.session(AsyncSession.class);
        session.executeReadAsync(tx -> tx.runAsync(
                        "MATCH (p:Person) RETURN p.name AS name LIMIT 10")

                .thenApplyAsync(res -> res.listAsync(row -> row.get("name")))
                .thenAcceptAsync(System.out::println)
                .exceptionallyAsync(e -> {
                    e.printStackTrace();
                    return null;
                })
        );
        // end::async[]
    }

    static void reactiveExample() {
        // tag::reactive[]
        Flux<String> names = Flux.usingWhen(
            Mono.just(driver.session(ReactiveSession.class)),
            session -> session.executeRead(tx -> 
                Mono.fromDirect(tx.run("MATCH (p:Person) RETURN p.name AS name LIMIT 10"))
                    .flatMapMany(ReactiveResult::records)
                    .map(record -> record.get("name").asString())
                    .doOnNext(System.out::println)
            ),
            session -> Mono.from(session.close())
        );
        // Optionally block to consume all results (for demonstration)
        names.then().block();
        // end::reactive[]
    }
}