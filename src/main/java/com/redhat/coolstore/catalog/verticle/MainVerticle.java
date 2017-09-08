package com.redhat.coolstore.catalog.verticle;

import com.redhat.coolstore.catalog.api.ApiVerticle;
import com.redhat.coolstore.catalog.verticle.service.CatalogService;
import com.redhat.coolstore.catalog.verticle.service.CatalogVerticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class MainVerticle extends AbstractVerticle {
	

	@Override
	public void start(Future<Void> startFuture) throws Exception {

		// Configure the `ConfigStoreOptions` instance with the name and the key of the configmap
		ConfigStoreOptions appStore = new ConfigStoreOptions();
		appStore.setType("configmap")
		.setFormat("yaml")
		.setConfig(new JsonObject()
				.put("name", "app-config")
				.put("key", "app-config.yml"));

		ConfigRetriever conf = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
																.addStore(appStore));
		
		conf.getConfig(ar -> {
			// If retrieval was successful, deploy the verticles
			if (ar.succeeded()) {
				deployVerticles(ar.result(), startFuture);
            } else{
            	startFuture.fail(ar.cause());;
            }
		});
	}

	private void deployVerticles(JsonObject config, Future<Void> startFuture) {

		DeploymentOptions options = new DeploymentOptions();
		options.setConfig(config);

		/*options.setConfig(new JsonObject().put("username","mongodb").put("password", "mongodb")
				.put("db_name", "catalogdb").
				put("connection_string", "mongodb://catalog-mongodb:27017")
				);
		 */
		
		Future<String> fu = Future.future();
		//Create an instance of `ApiVerticle` and using Proxy for `CatalogVerticle`
		ApiVerticle av = new ApiVerticle(CatalogService.createProxy(vertx));
		vertx.deployVerticle(av, options, fu.completer());

		Future<String> fu2 = Future.future();
		CatalogVerticle cv = new CatalogVerticle();
		vertx.deployVerticle(cv, options, fu2.completer());

		//`CompositeFuture` to coordinate the deployment of both verticles.
		CompositeFuture.all(fu, fu2).setHandler(ar1 -> {
			if (ar1.failed()) {
				System.out.println("failure");
				startFuture.fail(ar1.cause());
			}
			else {
				startFuture.complete();
			}
		});
	}

	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		super.stop(stopFuture);
	}

}
