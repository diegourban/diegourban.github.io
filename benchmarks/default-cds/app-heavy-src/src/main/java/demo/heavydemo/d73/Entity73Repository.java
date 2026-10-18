package demo.heavydemo.d73;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity73Repository extends JpaRepository<Entity73, Long> {
    Optional<Entity73> findByName(String name);
    Page<Entity73> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity73> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity73 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity73> search(String q);
}
