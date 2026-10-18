package demo.heavydemo.d65;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity65Repository extends JpaRepository<Entity65, Long> {
    Optional<Entity65> findByName(String name);
    Page<Entity65> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity65> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity65 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity65> search(String q);
}
