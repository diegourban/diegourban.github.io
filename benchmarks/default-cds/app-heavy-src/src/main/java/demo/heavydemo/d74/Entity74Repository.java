package demo.heavydemo.d74;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity74Repository extends JpaRepository<Entity74, Long> {
    Optional<Entity74> findByName(String name);
    Page<Entity74> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity74> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity74 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity74> search(String q);
}
