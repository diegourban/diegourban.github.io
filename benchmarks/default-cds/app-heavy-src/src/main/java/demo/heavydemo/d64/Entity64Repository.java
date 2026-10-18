package demo.heavydemo.d64;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity64Repository extends JpaRepository<Entity64, Long> {
    Optional<Entity64> findByName(String name);
    Page<Entity64> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity64> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity64 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity64> search(String q);
}
