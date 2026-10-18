package demo.heavydemo.d04;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity04Repository extends JpaRepository<Entity04, Long> {
    Optional<Entity04> findByName(String name);
    Page<Entity04> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity04> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity04 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity04> search(String q);
}
