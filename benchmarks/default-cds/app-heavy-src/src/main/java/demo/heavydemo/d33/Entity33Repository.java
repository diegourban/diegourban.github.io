package demo.heavydemo.d33;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity33Repository extends JpaRepository<Entity33, Long> {
    Optional<Entity33> findByName(String name);
    Page<Entity33> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity33> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity33 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity33> search(String q);
}
