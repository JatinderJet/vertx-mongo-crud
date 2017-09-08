package com.redhat.coolstore.catalog.verticle.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.redhat.coolstore.catalog.model.Product;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class CatalogServiceTest extends MongoTestBase {

	private Vertx vertx;

	@Before
	public void setup(TestContext context) throws Exception {
		vertx = Vertx.vertx();
		vertx.exceptionHandler(context.exceptionHandler());
		JsonObject config = getConfig();
		mongoClient = MongoClient.createNonShared(vertx, config);
		Async async = context.async();
		dropCollection(mongoClient, "products", async, context);
		async.await(10000);
		DeploymentOptions options = new DeploymentOptions().setConfig(config);
		vertx.deployVerticle(new CatalogVerticle(), options, context.asyncAssertSuccess());
		System.out.println("deployed verticle in setup method\n");
	}

	@After
	public void tearDown() throws Exception {
		mongoClient.close();
		vertx.close();
	}

	@Test
	public void testAddProduct(TestContext context) throws Exception {
		String itemId = "999999";
		String name = "productName";
		Product product = new Product();
		product.setItemId(itemId);
		product.setName(name);
		product.setDesc("productDescription");
		product.setPrice(100.0);

		CatalogService service = new CatalogServiceImpl(vertx, getConfig(), mongoClient);

		Async async = context.async();

		service.addProduct(product, ar -> {
			if (ar.failed()) {
				context.fail(ar.cause().getMessage());
			} else {
				JsonObject query = new JsonObject().put("_id", itemId);
				mongoClient.findOne("products", query, null, ar1 -> {
					if (ar1.failed()) {
						context.fail(ar1.cause().getMessage());
					} else {
						assertThat(ar1.result().getString("name"), equalTo(name));
						async.complete();
					}
				});
			}
		});
	}

	@Test
	public void testGetProducts(TestContext context) throws Exception {
		Async saveAsync = context.async(2);
		String itemId1 = "111111";
		JsonObject json1 = new JsonObject()
				.put("itemId", itemId1)
				.put("name", "productName1")
				.put("desc", "productDescription1")
				.put("price", new Double(100.0));

		String itemId2 = "222222";
		JsonObject json2 = new JsonObject()
				.put("itemId", itemId2)
				.put("name", "productName2")
				.put("desc", "productDescription2")
				.put("price", new Double(200.0));

		mongoClient.save("products", json1, ar -> {
			if (ar.failed()) {
				context.fail();
			}
			saveAsync.countDown();
		});

		mongoClient.save("products", json2, ar -> {
			if (ar.failed()) {
				context.fail();
			}
			saveAsync.countDown();
		});

		saveAsync.await();
		JsonObject jo = new JsonObject();
		DeliveryOptions options = new DeliveryOptions();
		options.addHeader("action", "getProducts");
		vertx.eventBus().<JsonArray>send(CatalogService.ADDRESS, jo, options, resp -> {
			assertThat(resp.failed(),CoreMatchers.equalTo(false));
			assertThat(resp.result(),CoreMatchers.notNullValue());

			Message<JsonArray> msg = resp.result();
			assertThat(msg.body(),CoreMatchers.notNullValue());
			JsonArray array = msg.body();
			assertThat(array.size(),CoreMatchers.equalTo(2));
			assertThat(array.getJsonObject(0).getString("itemId"),CoreMatchers.anyOf(equalTo("111111"), equalTo("222222")));
			saveAsync.complete();
		});

	}

	@Test
	public void testGetProduct(TestContext context) throws Exception {
		Async async = context.async();
		String itemId = "111111";
		JsonObject json = new JsonObject()
				.put("itemId", itemId)
				.put("name", "productName1")
				.put("desc", "productDescription1")
				.put("price", new Double(100.0));

		mongoClient.save("products", json, ar -> {
			if (ar.failed()) {
				context.fail();
			}else{
				CatalogService service = new CatalogServiceImpl(vertx, getConfig(), mongoClient);
				service.getProduct(itemId, res->{
					if(res.failed()){
						context.fail(ar.cause().getMessage());
					}else{
						assertThat(res.result().getItemId(),equalTo(itemId));
						async.complete();
					}

				});

			}
		});
	}

	//@Test
	public void testGetNonExistingProduct(TestContext context) throws Exception {
		Async async = context.async();
		String itemId = "111111";
		JsonObject json = new JsonObject()
				.put("itemId", itemId)
				.put("name", "productName1")
				.put("desc", "productDescription1")
				.put("price", new Double(100.0));

		mongoClient.save("products", json, ar -> {
			if (ar.failed()) {
				context.fail();
			}else{
				CatalogService service = new CatalogServiceImpl(vertx, getConfig(), mongoClient);
				service.getProduct(itemId, res->{
					if(res.failed()){
						context.fail(ar.cause().getMessage());
					}else{
						assertThat(res.result().getItemId(),equalTo(99999));
						async.complete();
					}

				});

			}
		});

	}

	@Test
	public void testPing(TestContext context) throws Exception {
		CatalogService service = new CatalogServiceImpl(vertx, getConfig(), mongoClient);

		Async async = context.async();
		service.ping(ar -> {
			assertThat(ar.succeeded(), equalTo(true));
			async.complete();
		});
	}

}
