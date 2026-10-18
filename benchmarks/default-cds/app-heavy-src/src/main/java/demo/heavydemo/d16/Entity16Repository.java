package demo.heavydemo.d16;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity16Repository extends JpaRepository<Entity16, Long> {
    Optional<Entity16> findByName(String name);
    Page<Entity16> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity16> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity16 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity16> search(String q);
}
