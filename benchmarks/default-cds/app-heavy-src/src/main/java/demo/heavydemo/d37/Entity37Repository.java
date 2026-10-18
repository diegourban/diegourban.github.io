package demo.heavydemo.d37;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity37Repository extends JpaRepository<Entity37, Long> {
    Optional<Entity37> findByName(String name);
    Page<Entity37> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity37> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity37 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity37> search(String q);
}
