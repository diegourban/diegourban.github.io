package demo.tx.orders;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Stock {

	@Id
	private String productId;

	private long quantity;

	protected Stock() {
	}

	Stock(String productId, long quantity) {
		this.productId = productId;
		this.quantity = quantity;
	}
}
