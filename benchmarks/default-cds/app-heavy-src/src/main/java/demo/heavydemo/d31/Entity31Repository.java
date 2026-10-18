package demo.heavydemo.d31;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity31Repository extends JpaRepository<Entity31, Long> {
    Optional<Entity31> findByName(String name);
    Page<Entity31> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity31> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity31 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity31> search(String q);
}
