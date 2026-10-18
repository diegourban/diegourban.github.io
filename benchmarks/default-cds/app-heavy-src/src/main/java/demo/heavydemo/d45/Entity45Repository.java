package demo.heavydemo.d45;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity45Repository extends JpaRepository<Entity45, Long> {
    Optional<Entity45> findByName(String name);
    Page<Entity45> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity45> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity45 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity45> search(String q);
}
