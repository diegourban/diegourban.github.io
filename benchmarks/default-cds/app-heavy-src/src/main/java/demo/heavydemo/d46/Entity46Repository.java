package demo.heavydemo.d46;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity46Repository extends JpaRepository<Entity46, Long> {
    Optional<Entity46> findByName(String name);
    Page<Entity46> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity46> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity46 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity46> search(String q);
}
