package demo.heavydemo.d35;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity35Repository extends JpaRepository<Entity35, Long> {
    Optional<Entity35> findByName(String name);
    Page<Entity35> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity35> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity35 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity35> search(String q);
}
