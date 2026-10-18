package demo.heavydemo.d18;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity18Repository extends JpaRepository<Entity18, Long> {
    Optional<Entity18> findByName(String name);
    Page<Entity18> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity18> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity18 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity18> search(String q);
}
