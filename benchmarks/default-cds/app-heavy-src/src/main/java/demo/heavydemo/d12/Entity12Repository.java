package demo.heavydemo.d12;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity12Repository extends JpaRepository<Entity12, Long> {
    Optional<Entity12> findByName(String name);
    Page<Entity12> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity12> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity12 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity12> search(String q);
}
