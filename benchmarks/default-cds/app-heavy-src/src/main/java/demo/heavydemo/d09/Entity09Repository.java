package demo.heavydemo.d09;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity09Repository extends JpaRepository<Entity09, Long> {
    Optional<Entity09> findByName(String name);
    Page<Entity09> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity09> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity09 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity09> search(String q);
}
