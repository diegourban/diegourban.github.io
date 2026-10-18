package demo.heavydemo.d24;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity24Repository extends JpaRepository<Entity24, Long> {
    Optional<Entity24> findByName(String name);
    Page<Entity24> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity24> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity24 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity24> search(String q);
}
