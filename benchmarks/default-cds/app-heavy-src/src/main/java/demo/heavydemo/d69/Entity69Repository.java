package demo.heavydemo.d69;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity69Repository extends JpaRepository<Entity69, Long> {
    Optional<Entity69> findByName(String name);
    Page<Entity69> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity69> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity69 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity69> search(String q);
}
