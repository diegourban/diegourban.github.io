package demo.heavydemo.d20;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity20Repository extends JpaRepository<Entity20, Long> {
    Optional<Entity20> findByName(String name);
    Page<Entity20> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity20> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity20 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity20> search(String q);
}
