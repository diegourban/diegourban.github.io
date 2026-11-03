package demo.tx.orders;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The reference refactor: no transaction around the method, one around the two writes only. */
@Service
public class NarrowedOrderService {

	private final Writer writer;

	private final OrderService orders;

	NarrowedOrderService(Writer writer, OrderService orders) {
		this.writer = writer;
		this.orders = orders;
	}

	public String placeOrder(String productId, int quantity) {
		if (OrderService.isOwnStock(productId)) {
			return "placed:" + writer.write(productId, quantity);
		}
		return "forwarded:" + orders.forward(productId, quantity);
	}

	@Service
	static class Writer {

		private final OrderRepository orders;

		private final StockRepository stock;

		Writer(OrderRepository orders, StockRepository stock) {
			this.orders = orders;
			this.stock = stock;
		}

		@Transactional
		Long write(String productId, int quantity) {
			Long id = orders.save(new PurchaseOrder(productId, quantity)).getId();
			stock.decrement(productId, quantity);
			return id;
		}
	}
}
