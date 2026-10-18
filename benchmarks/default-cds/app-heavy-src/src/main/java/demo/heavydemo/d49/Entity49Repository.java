package demo.heavydemo.d49;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity49Repository extends JpaRepository<Entity49, Long> {
    Optional<Entity49> findByName(String name);
    Page<Entity49> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity49> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity49 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity49> search(String q);
}
