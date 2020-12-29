package io.vertx.example.web.graphql;

import graphql.GraphQL;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.reactivex.Flowable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.graphql.ApolloWSHandler;
import io.vertx.reactivex.RxHelper;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

public class SubscriptionServer extends AbstractVerticle {

  public static void main(String[] args) {
    Launcher.executeCommand("run", SubscriptionServer.class.getName());
  }

  private List<Link> links;

  @Override
  public void start() {
    prepareData();

    Router router = Router.router(vertx);
    router.route("/graphql").handler(ApolloWSHandler.create(createGraphQL()));

    HttpServerOptions httpServerOptions = new HttpServerOptions()
      .setWebsocketSubProtocols("graphql-ws");
    vertx.createHttpServer(httpServerOptions)
      .requestHandler(router)
      .listen(8080);
  }

  private void prepareData() {
    User peter = new User("Peter");
    User paul = new User("Paul");
    User jack = new User("Jack");

    links = new ArrayList<>();
    links.add(new Link("https://vertx.io", "Vert.x project", peter));
    links.add(new Link("https://www.eclipse.org", "Eclipse Foundation", paul));
    links.add(new Link("http://reactivex.io", "ReactiveX libraries", jack));
    links.add(new Link("https://www.graphql-java.com", "GraphQL Java implementation", peter));
  }

  private GraphQL createGraphQL() {
    String schema = vertx.fileSystem().readFileBlocking("links.graphqls").toString();

    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

    RuntimeWiring runtimeWiring = newRuntimeWiring()
      .type("Subscription", builder -> builder.dataFetcher("links", this::linksFetcher))
      .build();

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

    return GraphQL.newGraphQL(graphQLSchema)
      .build();
  }

  private Publisher<Link> linksFetcher(DataFetchingEnvironment env) {
    return Flowable.interval(1, TimeUnit.SECONDS) // Ticks
      .zipWith(Flowable.fromIterable(links), (tick, link) -> link) // Emit link on each tick
      .observeOn(RxHelper.scheduler(context)); // Observe on the verticle context thread
  }
}
