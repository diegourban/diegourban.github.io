package demo.tx.orders;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class PurchaseOrder {

	@Id
	@GeneratedValue
	private Long id;

	private String productId;

	private int quantity;

	protected PurchaseOrder() {
	}

	PurchaseOrder(String productId, int quantity) {
		this.productId = productId;
		this.quantity = quantity;
	}

	public Long getId() {
		return id;
	}
}
