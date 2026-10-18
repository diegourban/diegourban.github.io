package demo.heavydemo.d71;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity71Repository extends JpaRepository<Entity71, Long> {
    Optional<Entity71> findByName(String name);
    Page<Entity71> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity71> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity71 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity71> search(String q);
}
