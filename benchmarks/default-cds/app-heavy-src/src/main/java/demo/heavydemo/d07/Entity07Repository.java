package demo.heavydemo.d07;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity07Repository extends JpaRepository<Entity07, Long> {
    Optional<Entity07> findByName(String name);
    Page<Entity07> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity07> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity07 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity07> search(String q);
}
