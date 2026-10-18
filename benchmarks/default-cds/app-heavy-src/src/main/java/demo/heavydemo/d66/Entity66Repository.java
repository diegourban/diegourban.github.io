package demo.heavydemo.d66;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity66Repository extends JpaRepository<Entity66, Long> {
    Optional<Entity66> findByName(String name);
    Page<Entity66> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity66> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity66 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity66> search(String q);
}
