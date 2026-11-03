package demo.tx.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface OrderRepository extends JpaRepository<PurchaseOrder, Long> {
}

interface StockRepository extends JpaRepository<Stock, String> {

	@Modifying
	@Query("update Stock s set s.quantity = s.quantity - :quantity where s.productId = :productId")
	int decrement(String productId, int quantity);
}
