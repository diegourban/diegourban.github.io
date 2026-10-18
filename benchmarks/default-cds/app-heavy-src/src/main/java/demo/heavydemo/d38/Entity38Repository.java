package demo.heavydemo.d38;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity38Repository extends JpaRepository<Entity38, Long> {
    Optional<Entity38> findByName(String name);
    Page<Entity38> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity38> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity38 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity38> search(String q);
}
