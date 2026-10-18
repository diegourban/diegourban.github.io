package demo.heavydemo.d39;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity39Repository extends JpaRepository<Entity39, Long> {
    Optional<Entity39> findByName(String name);
    Page<Entity39> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity39> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity39 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity39> search(String q);
}
