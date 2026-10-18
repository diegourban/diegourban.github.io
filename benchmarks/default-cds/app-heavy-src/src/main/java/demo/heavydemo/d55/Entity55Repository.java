package demo.heavydemo.d55;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity55Repository extends JpaRepository<Entity55, Long> {
    Optional<Entity55> findByName(String name);
    Page<Entity55> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity55> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity55 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity55> search(String q);
}
