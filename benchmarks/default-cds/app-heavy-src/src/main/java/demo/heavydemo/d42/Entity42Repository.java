package demo.heavydemo.d42;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity42Repository extends JpaRepository<Entity42, Long> {
    Optional<Entity42> findByName(String name);
    Page<Entity42> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity42> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity42 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity42> search(String q);
}
