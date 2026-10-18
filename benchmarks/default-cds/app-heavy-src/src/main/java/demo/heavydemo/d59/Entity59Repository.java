package demo.heavydemo.d59;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity59Repository extends JpaRepository<Entity59, Long> {
    Optional<Entity59> findByName(String name);
    Page<Entity59> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity59> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity59 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity59> search(String q);
}
