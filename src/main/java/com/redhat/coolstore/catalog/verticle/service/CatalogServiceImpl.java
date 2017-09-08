package com.redhat.coolstore.catalog.verticle.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.redhat.coolstore.catalog.model.Product;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class CatalogServiceImpl implements CatalogService {

	private MongoClient client;

	public CatalogServiceImpl(Vertx vertx, JsonObject config, MongoClient client) {
		this.client = client;
	}

	@Override
	public void getProducts(Handler<AsyncResult<List<Product>>> resulthandler) {
		// Pass  empty JSONObject for query to `MongoClient.find()` method
		// Using streams to transform the `List<JSONObject>` to `List<Person>`
		client.find("products", new JsonObject(), ar -> {
			if(ar.succeeded()){
				List <JsonObject> listJsonObj = ar.result();
				List<Product> products =  listJsonObj.stream().map(Product::new).collect(Collectors.toList());
				resulthandler.handle(Future.succeededFuture(products));
			}else {
				resulthandler.handle(Future.failedFuture(ar.cause()));
			}

		});



	}

	@Override
	public void getProduct(String itemId, Handler<AsyncResult<Product>> resulthandler) {
		// Pass  JSONObject with field `itemId` for query to `MongoClient.find()` method
		// Using streams to transform the `List<JSONObject>` to `Person`
		JsonObject query = new JsonObject().put("itemId", itemId);
		client.find("products", query, ar -> {
			if(ar.succeeded()){
				if(ar.result() == null){
					System.out.println("No Match Found for itemId:"+itemId);
					resulthandler.handle(Future.succeededFuture(null));

				}else{
					List <JsonObject> listJsonObj = ar.result();
					Product product =  listJsonObj.stream().map(Product::new).collect(Collectors.toList()).get(0);
					resulthandler.handle(Future.succeededFuture(product));
				}

			}else{
				resulthandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}

	@Override
	public void addProduct(Product product, Handler<AsyncResult<String>> resulthandler) {
		client.save("products", toDocument(product), resulthandler);
	}

	@Override
	public void ping(Handler<AsyncResult<String>> resultHandler) {
		resultHandler.handle(Future.succeededFuture("OK"));
	}

	private JsonObject toDocument(Product product) {
		JsonObject document = product.toJson();
		document.put("_id", product.getItemId());
		return document;
	}
}
