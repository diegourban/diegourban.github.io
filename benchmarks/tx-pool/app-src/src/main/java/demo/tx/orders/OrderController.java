package demo.tx.orders;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

	private final OrderService orders;

	private final NarrowedOrderService narrowed;

	OrderController(OrderService orders, NarrowedOrderService narrowed) {
		this.orders = orders;
		this.narrowed = narrowed;
	}

	@PostMapping("/orders")
	String placeOrder(@RequestParam String product, @RequestParam(defaultValue = "1") int quantity) {
		return orders.placeOrder(product, quantity);
	}

	@PostMapping("/orders-narrowed")
	String placeOrderNarrowed(@RequestParam String product, @RequestParam(defaultValue = "1") int quantity) {
		return narrowed.placeOrder(product, quantity);
	}
}
