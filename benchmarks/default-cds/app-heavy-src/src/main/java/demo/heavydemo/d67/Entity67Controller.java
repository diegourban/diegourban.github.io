package demo.heavydemo.d67;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity67")
public class Entity67Controller {
    private final Entity67Service service;
    Entity67Controller(Entity67Service service) { this.service = service; }
    @GetMapping List<Entity67> all() { return service.all(); }
    @GetMapping("/search") List<Entity67> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity67 create(@Valid @RequestBody Entity67 e) { return service.create(e); }
}
