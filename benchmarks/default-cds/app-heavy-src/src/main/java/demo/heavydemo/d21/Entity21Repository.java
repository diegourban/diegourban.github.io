package demo.heavydemo.d21;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity21Repository extends JpaRepository<Entity21, Long> {
    Optional<Entity21> findByName(String name);
    Page<Entity21> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity21> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity21 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity21> search(String q);
}
