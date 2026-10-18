package demo.heavydemo.d10;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity10Repository extends JpaRepository<Entity10, Long> {
    Optional<Entity10> findByName(String name);
    Page<Entity10> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity10> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity10 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity10> search(String q);
}
