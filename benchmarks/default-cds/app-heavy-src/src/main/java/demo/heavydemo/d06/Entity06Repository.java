package demo.heavydemo.d06;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity06Repository extends JpaRepository<Entity06, Long> {
    Optional<Entity06> findByName(String name);
    Page<Entity06> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity06> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity06 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity06> search(String q);
}
