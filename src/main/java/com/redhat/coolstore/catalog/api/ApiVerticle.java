package com.redhat.coolstore.catalog.api;

import java.util.List;


import com.redhat.coolstore.catalog.model.Product;
import com.redhat.coolstore.catalog.verticle.service.CatalogService;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class ApiVerticle extends AbstractVerticle {

	private CatalogService catalogService;

	public ApiVerticle(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@Override
	public void start(Future<Void> startFuture) throws Exception {

		Router router = Router.router(vertx);
		
		// Add routes to the Router
		// GET requests that matches the "/products" path
		router.get("/products").handler(this::getProducts);
		// GET requests that matches the /product/:itemId path
		router.get("/product/:itemId").handler(this::getProduct);
		// POST requests that matches the "/product" path
		router.route("/product").handler(BodyHandler.create());
		router.post("/product").handler(this::addProduct);
		
		// Read port from config
		int portnum = config().getInteger("catalog.http.port", 8080);

		// Create a HTTP server
		vertx.createHttpServer().requestHandler(router::accept).listen(portnum, ar -> {
			if (ar.failed()) {
				startFuture.fail(ar.cause());
			}
			else {
				startFuture.complete();
			}
		});

	}

	private void getProducts(RoutingContext rc) {
		/*	JsonObject jo = new JsonObject();
		DeliveryOptions options = new DeliveryOptions();
		options.addHeader("action", "getProducts");
		vertx.eventBus().<JsonArray>send(CatalogService.ADDRESS, jo, options, resp -> {
			if (resp.failed()) {
				resp.cause().printStackTrace();
				rc.fail(503);
			}
			else {
				Message<JsonArray> msg = resp.result();
				JsonArray arrJsonResult = msg.body();
				rc.response().putHeader("Content-Type", "application/json; charset=utf-8").end(arrJsonResult.encodePrettily());
			}
		});*/
		
		// Calls the `getProducts()` method of the CatalogService
		catalogService.getProducts(resp -> {
			if (resp.failed()) {
				resp.cause().printStackTrace();
				rc.fail(503);
			}
			else {
				JsonArray arrProduct = new JsonArray();
				resp.result().stream().forEach(j -> { arrProduct.add(j.toJson());});
				rc.response().putHeader("Content-Type", "application/json; charset=utf-8").end(arrProduct.encodePrettily());
			}
		});

	}

	private void getProduct(RoutingContext rc) {
        	// Read the passed itemId
		String ietmId = rc.request().getParam("itemId");
		
		/*DeliveryOptions options = new DeliveryOptions();
		options.addHeader("action", "getProduct");
		vertx.eventBus().<JsonObject>send(CatalogService.ADDRESS, new JsonObject().put("itemId", ietmId), options, resp -> {
			if (resp.failed()) {
				resp.cause().printStackTrace();
				rc.fail(503);
			}
			else {
				if(resp.result() == null){
					rc.fail(404);
				}else
					rc.response().putHeader("Content-Type", "application/json; charset=utf-8").end(resp.result().body().encodePrettily());
			}
		});*/
		
		// Call the `getProduct()` method of the CatalogService
		catalogService.getProduct(ietmId,resp -> {
			if (resp.failed()) {
				resp.cause().printStackTrace();
				rc.fail(503);
			}
			else {
				if(resp.result() == null){
					rc.fail(404);
				}else
					rc.response().putHeader("Content-Type", "application/json; charset=utf-8").end(resp.result().toJson().encodePrettily());
			}
		});
	}

	private void addProduct(RoutingContext rc) {
		/*DeliveryOptions options = new DeliveryOptions();
		options.addHeader("action", "addProduct");
		vertx.eventBus().<JsonObject>send(CatalogService.ADDRESS, new JsonObject().put("product", rc.getBodyAsJson()), options, resp -> {
			if(resp.succeeded()){
				rc.response().setStatusCode(201).putHeader("Content-Type", "application/json; charset=utf-8").end();				
			}else{
				rc.fail(resp.cause());
			}

		});*/
		// Call the `addProduct()` method of the CatalogService, on success sets HTTP code 201.
		// Transform JSON payload to `Product` object
		catalogService.addProduct(new Product(rc.getBodyAsJson()), resp->{
			if(resp.succeeded()){
				rc.response().setStatusCode(201).putHeader("Content-Type", "application/json; charset=utf-8").end();				
			}else{
				rc.fail(resp.cause());
			}
		});

	}

}
