package demo.heavydemo.d14;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity14Repository extends JpaRepository<Entity14, Long> {
    Optional<Entity14> findByName(String name);
    Page<Entity14> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity14> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity14 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity14> search(String q);
}
