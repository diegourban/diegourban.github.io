package demo.heavydemo.d26;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity26Repository extends JpaRepository<Entity26, Long> {
    Optional<Entity26> findByName(String name);
    Page<Entity26> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity26> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity26 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity26> search(String q);
}
