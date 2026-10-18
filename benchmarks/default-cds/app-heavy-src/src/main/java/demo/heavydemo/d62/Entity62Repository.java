package demo.heavydemo.d62;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity62Repository extends JpaRepository<Entity62, Long> {
    Optional<Entity62> findByName(String name);
    Page<Entity62> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity62> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity62 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity62> search(String q);
}
