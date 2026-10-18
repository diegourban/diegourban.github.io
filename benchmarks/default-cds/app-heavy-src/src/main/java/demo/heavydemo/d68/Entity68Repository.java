package demo.heavydemo.d68;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity68Repository extends JpaRepository<Entity68, Long> {
    Optional<Entity68> findByName(String name);
    Page<Entity68> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity68> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity68 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity68> search(String q);
}
