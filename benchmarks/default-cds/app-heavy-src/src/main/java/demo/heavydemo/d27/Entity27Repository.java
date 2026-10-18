package demo.heavydemo.d27;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity27Repository extends JpaRepository<Entity27, Long> {
    Optional<Entity27> findByName(String name);
    Page<Entity27> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity27> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity27 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity27> search(String q);
}
