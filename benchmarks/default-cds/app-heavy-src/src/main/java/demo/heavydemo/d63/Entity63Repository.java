package demo.heavydemo.d63;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity63Repository extends JpaRepository<Entity63, Long> {
    Optional<Entity63> findByName(String name);
    Page<Entity63> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity63> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity63 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity63> search(String q);
}
