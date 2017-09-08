package com.redhat.coolstore.catalog.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.coolstore.catalog.model.Product;
import com.redhat.coolstore.catalog.verticle.service.CatalogService;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class ApiVerticleTest {

	private Vertx vertx;
	private Integer port;
	private CatalogService catalogService;

	@Before
	public void setUp(TestContext context) throws IOException {
		vertx = Vertx.vertx();

		// Register the context exception handler
		vertx.exceptionHandler(context.exceptionHandler());

		// Let's configure the verticle to listen on the 'test' port (randomly picked).
		// We create deployment options and set the _configuration_ json object:
		ServerSocket socket = new ServerSocket(0);
		port = socket.getLocalPort();
		socket.close();

		DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("catalog.http.port", port));

		//Mock the catalog Service
		catalogService = mock(CatalogService.class);
		
		// wait for verticle has successfully completed its start sequence using `context.asyncAssertSuccess`
		vertx.deployVerticle(new ApiVerticle(catalogService), options, context.asyncAssertSuccess());
	}

	/**
	 * This method, called after our test, just cleanup everything by closing
	 * the vert.x instance
	 *
	 * @param context
	 *            the test context
	 */
	@After
	public void tearDown(TestContext context) {
		vertx.close(context.asyncAssertSuccess());
	}

	@Test
	public void testGetProducts(TestContext context) throws Exception {
		List<Product> listProduct = new ArrayList<Product>();
		Product pr1 = new Product();
		pr1.setItemId("1111");
		pr1.setDesc("pr1Desc");
		pr1.setName("pr1Name");
		pr1.setPrice(100.0);
		listProduct.add(pr1);
		
		Product pr2 = new Product();
		pr2.setItemId("2222");
		pr2.setDesc("pr2Desc");
		pr2.setName("pr2Name");
		pr2.setPrice(100.0);
		listProduct.add(pr2);
		
		// Stub the `getProducts()` method of `CatalogService` mock to return a `List<Product>`
		doAnswer(new Answer<Void>() {
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Handler<AsyncResult<List<Product>>> handler = invocation.getArgument(0);

				handler.handle(Future.succeededFuture(listProduct));
				return null;
			}
		}).when(catalogService).getProducts(any());


		Async async = context.async();
		// Use the Vert.x Web client to execute a GET request to the "/products" endpoint
		vertx.createHttpClient().getNow(port, "localhost", "/products", response -> {
			assertThat(response.statusCode(), equalTo(200));
			assertThat(response.headers().get("content-type"), equalTo("application/json; charset=utf-8"));
			response.bodyHandler(body -> {
				assertThat(body.toJsonArray().getJsonObject(0).getValue("itemId"),equalTo(pr1.getItemId()));
				async.complete();
			});
		});

	}


	@Test
	public void testGetProduct(TestContext context) throws Exception {

		Product pr2 = new Product();
		pr2.setItemId("2222");
		pr2.setDesc("pr2Desc");
		pr2.setName("pr2Name");
		pr2.setPrice(100.0);
		
		// Stub the `getProduct` method of `CatalogService` mock to return a `Product`
		doAnswer(new Answer<Void>() {
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Handler<AsyncResult<Product>> handler = invocation.getArgument(1);

				handler.handle(Future.succeededFuture(pr2));
				return null;
			}
		}).when(catalogService).getProduct(any(),any());


		Async async = context.async();
		// Use the Vert.x Web client to execute a GET request to the "/product/:itemId" endpoint
		vertx.createHttpClient().get(port, "localhost", "/product/:itemId")
		.handler(response -> {
			assertThat(response.statusCode(), equalTo(200));
			assertThat(response.headers().get("content-type"), equalTo("application/json; charset=utf-8"));
			response.bodyHandler(body -> {
				assertThat(body.toJsonObject().getValue("itemId"),equalTo(pr2.getItemId()));
				async.complete();
			});
		})
		.putHeader("Content-length", Integer.toString(pr2.getItemId().length()))
		.write(pr2.getItemId())
		.end();
	}

	//@Test
	public void testGetNonExistingProduct(TestContext context) throws Exception {
		//----
		// To be implemented
		//
		//----
	}

	@Test
	public void testAddProduct(TestContext context) throws Exception {
		doAnswer(new Answer<Void>() {
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Handler<AsyncResult<String>> handler = invocation.getArgument(1);
				handler.handle(Future.succeededFuture(null));
				return null;
			}
		}).when(catalogService).addProduct(any(),any());

		Async async = context.async();
		String itemId = "111111";
		JsonObject json = new JsonObject()
				.put("itemId", itemId)
				.put("name", "productName")
				.put("desc", "productDescription")
				.put("price", new Double(100.0));
		String body = json.encodePrettily();
		String length = Integer.toString(body.length());

		vertx.createHttpClient().post(port, "localhost", "/product")
		.exceptionHandler(context.exceptionHandler())
		.putHeader("Content-type", "application/json")
		.putHeader("Content-length", length)
		.handler(response -> {
			assertThat(response.statusCode(), equalTo(201));
			ArgumentCaptor<Product> argument = ArgumentCaptor.forClass(Product.class);
			verify(catalogService).addProduct(argument.capture(), any());
			assertThat(argument.getValue().getItemId(), equalTo(itemId));
			async.complete();
		})
		.write(body)
		.end();
	}

}
