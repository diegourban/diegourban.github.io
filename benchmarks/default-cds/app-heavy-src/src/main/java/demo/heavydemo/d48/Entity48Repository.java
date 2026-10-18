package demo.heavydemo.d48;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity48Repository extends JpaRepository<Entity48, Long> {
    Optional<Entity48> findByName(String name);
    Page<Entity48> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity48> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity48 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity48> search(String q);
}
