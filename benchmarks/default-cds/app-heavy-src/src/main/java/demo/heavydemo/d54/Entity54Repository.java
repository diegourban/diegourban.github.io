package demo.heavydemo.d54;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity54Repository extends JpaRepository<Entity54, Long> {
    Optional<Entity54> findByName(String name);
    Page<Entity54> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity54> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity54 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity54> search(String q);
}
