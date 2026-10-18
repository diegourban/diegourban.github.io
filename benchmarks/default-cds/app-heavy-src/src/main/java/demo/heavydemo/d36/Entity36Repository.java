package demo.heavydemo.d36;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity36Repository extends JpaRepository<Entity36, Long> {
    Optional<Entity36> findByName(String name);
    Page<Entity36> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity36> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity36 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity36> search(String q);
}
