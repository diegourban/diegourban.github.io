package demo.heavydemo.d77;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity77Repository extends JpaRepository<Entity77, Long> {
    Optional<Entity77> findByName(String name);
    Page<Entity77> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity77> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity77 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity77> search(String q);
}
