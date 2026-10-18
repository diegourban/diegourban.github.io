package demo.heavydemo.d75;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity75Repository extends JpaRepository<Entity75, Long> {
    Optional<Entity75> findByName(String name);
    Page<Entity75> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity75> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity75 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity75> search(String q);
}
