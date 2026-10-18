package demo.heavydemo.d37;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity37")
public class Entity37Controller {
    private final Entity37Service service;
    Entity37Controller(Entity37Service service) { this.service = service; }
    @GetMapping List<Entity37> all() { return service.all(); }
    @GetMapping("/search") List<Entity37> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity37 create(@Valid @RequestBody Entity37 e) { return service.create(e); }
}
