package demo.heavydemo.d01;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity01Repository extends JpaRepository<Entity01, Long> {
    Optional<Entity01> findByName(String name);
    Page<Entity01> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity01> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity01 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity01> search(String q);
}
