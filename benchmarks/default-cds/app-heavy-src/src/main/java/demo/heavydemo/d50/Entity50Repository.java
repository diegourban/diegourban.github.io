package demo.heavydemo.d50;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity50Repository extends JpaRepository<Entity50, Long> {
    Optional<Entity50> findByName(String name);
    Page<Entity50> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity50> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity50 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity50> search(String q);
}
