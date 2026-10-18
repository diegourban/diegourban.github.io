package demo.heavydemo.d13;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity13Repository extends JpaRepository<Entity13, Long> {
    Optional<Entity13> findByName(String name);
    Page<Entity13> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity13> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity13 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity13> search(String q);
}
