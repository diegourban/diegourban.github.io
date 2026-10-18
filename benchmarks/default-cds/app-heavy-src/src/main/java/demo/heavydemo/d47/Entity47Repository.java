package demo.heavydemo.d47;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity47Repository extends JpaRepository<Entity47, Long> {
    Optional<Entity47> findByName(String name);
    Page<Entity47> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity47> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity47 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity47> search(String q);
}
