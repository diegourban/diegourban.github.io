package demo.heavydemo.d52;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity52Repository extends JpaRepository<Entity52, Long> {
    Optional<Entity52> findByName(String name);
    Page<Entity52> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity52> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity52 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity52> search(String q);
}
