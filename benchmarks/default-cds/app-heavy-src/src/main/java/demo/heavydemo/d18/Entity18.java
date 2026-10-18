package demo.heavydemo.d18;

import demo.heavydemo.d17.Entity17;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "entity18")
public class Entity18 {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @NotBlank @Size(max = 120) String name;
    @Size(max = 500) String description;
    @NotNull String status;
    @PositiveOrZero BigDecimal amount;
    LocalDate effectiveDate;
    Instant createdAt;
    @Version Long version;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id") Entity17 parent;
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
