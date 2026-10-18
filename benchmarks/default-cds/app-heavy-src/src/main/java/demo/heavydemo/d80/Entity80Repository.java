package demo.heavydemo.d80;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity80Repository extends JpaRepository<Entity80, Long> {
    Optional<Entity80> findByName(String name);
    Page<Entity80> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity80> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity80 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity80> search(String q);
}
