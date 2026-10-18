package demo.heavydemo.d19;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity19Repository extends JpaRepository<Entity19, Long> {
    Optional<Entity19> findByName(String name);
    Page<Entity19> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity19> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity19 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity19> search(String q);
}
