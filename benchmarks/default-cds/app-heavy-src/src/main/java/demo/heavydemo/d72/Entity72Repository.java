package demo.heavydemo.d72;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity72Repository extends JpaRepository<Entity72, Long> {
    Optional<Entity72> findByName(String name);
    Page<Entity72> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity72> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity72 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity72> search(String q);
}
