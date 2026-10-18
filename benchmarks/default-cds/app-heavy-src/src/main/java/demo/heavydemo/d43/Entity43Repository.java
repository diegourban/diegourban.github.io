package demo.heavydemo.d43;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity43Repository extends JpaRepository<Entity43, Long> {
    Optional<Entity43> findByName(String name);
    Page<Entity43> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity43> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity43 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity43> search(String q);
}
