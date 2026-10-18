package demo.heavydemo.d44;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity44Repository extends JpaRepository<Entity44, Long> {
    Optional<Entity44> findByName(String name);
    Page<Entity44> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity44> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity44 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity44> search(String q);
}
