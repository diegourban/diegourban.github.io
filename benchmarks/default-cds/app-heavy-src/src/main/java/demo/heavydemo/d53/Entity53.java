package demo.heavydemo.d53;

import demo.heavydemo.d52.Entity52;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "entity53")
public class Entity53 {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @NotBlank @Size(max = 120) String name;
    @Size(max = 500) String description;
    @NotNull String status;
    @PositiveOrZero BigDecimal amount;
    LocalDate effectiveDate;
    Instant createdAt;
    @Version Long version;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id") Entity52 parent;
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
