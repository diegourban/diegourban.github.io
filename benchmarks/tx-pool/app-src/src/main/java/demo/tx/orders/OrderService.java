package demo.tx.orders;

import java.net.http.HttpClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * The method under load: an order for a product we stock is two writes (the order and the stock), so it needs a
 * transaction; an order for a marketplace product is forwarded to the partner's API and never touches the database.
 * Calling a third-party API from inside a request like this is only for the demonstration; asynchronous calls or an
 * outbox are usually the better design.
 */
@Service
public class OrderService {

	static final int OWN_PRODUCTS = 100;

	private final OrderRepository orders;

	private final StockRepository stock;

	private final RestClient partnerApi;

	OrderService(OrderRepository orders, StockRepository stock, RestClient.Builder builder,
			@Value("${partner.url:http://wiremock:8080/partner/orders}") String partnerUrl) {
		this.orders = orders;
		this.stock = stock;
		// HTTP/1.1: the JDK client's HTTP/2 upgrade on a POST is reset by WireMock (RST_STREAM).
		HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
		this.partnerApi = builder.baseUrl(partnerUrl).requestFactory(new JdkClientHttpRequestFactory(http)).build();
	}

	@Transactional
	public String placeOrder(String productId, int quantity) {
		if (isOwnStock(productId)) {
			PurchaseOrder order = orders.save(new PurchaseOrder(productId, quantity));
			stock.decrement(productId, quantity);
			return "placed:" + order.getId();
		}
		return "forwarded:" + forward(productId, quantity);
	}

	/** The catalog rule lives in memory (in a real app, a cache): a query here would put every order in case (c). */
	static boolean isOwnStock(String productId) {
		return productId.startsWith("own-");
	}

	String forward(String productId, int quantity) {
		return partnerApi.post().body(new PartnerOrder(productId, quantity)).retrieve().body(String.class);
	}

	record PartnerOrder(String productId, int quantity) {
	}

	/** Seeds the stock rows once, so the decrements spread over many rows instead of contending on one. */
	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void seedStock() {
		if (stock.count() == 0) {
			for (int i = 0; i < OWN_PRODUCTS; i++) {
				stock.save(new Stock("own-" + i, 1_000_000_000L));
			}
		}
	}
}
