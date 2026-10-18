package demo.heavydemo.d58;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity58Repository extends JpaRepository<Entity58, Long> {
    Optional<Entity58> findByName(String name);
    Page<Entity58> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity58> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity58 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity58> search(String q);
}
