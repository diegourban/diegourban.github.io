package demo.heavydemo.d11;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity11Repository extends JpaRepository<Entity11, Long> {
    Optional<Entity11> findByName(String name);
    Page<Entity11> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity11> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity11 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity11> search(String q);
}
