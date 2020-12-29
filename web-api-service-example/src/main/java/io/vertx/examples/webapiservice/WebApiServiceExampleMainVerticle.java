package io.vertx.examples.webapiservice;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.examples.webapiservice.persistence.TransactionPersistence;
import io.vertx.examples.webapiservice.services.TransactionsManagerService;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.serviceproxy.ServiceBinder;

public class WebApiServiceExampleMainVerticle extends AbstractVerticle {

  HttpServer server;
  ServiceBinder serviceBinder;

  MessageConsumer<JsonObject> consumer;

  /**
   * Start transaction service
   */
  private void startTransactionService() {
    serviceBinder = new ServiceBinder(vertx);

    TransactionPersistence persistence = TransactionPersistence.create();

    // Create an instance of TransactionManagerService and mount to event bus
    TransactionsManagerService transactionsManagerService = TransactionsManagerService.create(persistence);
    consumer = serviceBinder
        .setAddress("transactions_manager.myapp")
        .register(TransactionsManagerService.class, transactionsManagerService);
  }

  /**
   * This method constructs the router factory, mounts services and handlers and starts the http server with built router
   * @return
   */
  private Future<Void> startHttpServer() {
    Promise<Void> promise = Promise.promise();
    OpenAPI3RouterFactory.create(this.vertx, "/openapi.json", openAPI3RouterFactoryAsyncResult -> {
      if (openAPI3RouterFactoryAsyncResult.succeeded()) {
        OpenAPI3RouterFactory routerFactory = openAPI3RouterFactoryAsyncResult.result();

        // Mount services on event bus based on extensions
        routerFactory.mountServicesFromExtensions();

        // Generate the router
        Router router = routerFactory.getRouter();
        server = vertx.createHttpServer(new HttpServerOptions().setPort(8080).setHost("localhost"));
        server.requestHandler(router).listen(ar -> {
          // Error starting the HttpServer
          if (ar.succeeded()) promise.complete();
          else promise.fail(ar.cause());
        });
      } else {
        // Something went wrong during router factory initialization
        promise.fail(openAPI3RouterFactoryAsyncResult.cause());
      }
    });
    return promise.future();
  }

  @Override
  public void start(Promise<Void> promise) {
    startTransactionService();
    startHttpServer().onComplete(promise);
  }

  /**
   * This method closes the http server and unregister all services loaded to Event Bus
   */
  @Override
  public void stop(){
    this.server.close();
    consumer.unregister();
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new WebApiServiceExampleMainVerticle());
  }

}
