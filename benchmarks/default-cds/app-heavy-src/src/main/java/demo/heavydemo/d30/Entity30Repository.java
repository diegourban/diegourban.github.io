package demo.heavydemo.d30;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity30Repository extends JpaRepository<Entity30, Long> {
    Optional<Entity30> findByName(String name);
    Page<Entity30> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity30> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity30 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity30> search(String q);
}
