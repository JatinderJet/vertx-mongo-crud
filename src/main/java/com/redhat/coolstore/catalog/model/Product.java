package com.redhat.coolstore.catalog.model;

import java.io.Serializable;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class Product implements Serializable {

	private static final long serialVersionUID = -6994655395272795259L;

	private String itemId;
	private String name;
	private String desc;
	private double price;

	public Product() {

	}

	public Product(JsonObject obj){
		itemId = obj.getString("itemId");
		name = obj.getString("name");
		desc = obj.getString("desc");
		price = obj.getDouble("price");

	}

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public JsonObject toJson() {
		JsonObject obj = new JsonObject();
		obj.put("itemId", itemId);
		obj.put("name", name);
		obj.put("desc", desc);
		obj.put("price", price);
		
		return obj;
	}
}
