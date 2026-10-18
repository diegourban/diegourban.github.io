package demo.heavydemo.d70;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity70Repository extends JpaRepository<Entity70, Long> {
    Optional<Entity70> findByName(String name);
    Page<Entity70> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity70> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity70 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity70> search(String q);
}
