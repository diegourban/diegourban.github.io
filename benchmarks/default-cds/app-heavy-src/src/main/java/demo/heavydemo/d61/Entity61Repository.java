package demo.heavydemo.d61;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity61Repository extends JpaRepository<Entity61, Long> {
    Optional<Entity61> findByName(String name);
    Page<Entity61> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity61> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity61 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity61> search(String q);
}
