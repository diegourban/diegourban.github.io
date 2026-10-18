package demo.heavydemo.d32;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity32Repository extends JpaRepository<Entity32, Long> {
    Optional<Entity32> findByName(String name);
    Page<Entity32> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity32> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity32 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity32> search(String q);
}
