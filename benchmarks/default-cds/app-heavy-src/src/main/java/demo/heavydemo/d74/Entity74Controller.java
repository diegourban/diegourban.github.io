package demo.heavydemo.d74;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity74")
public class Entity74Controller {
    private final Entity74Service service;
    Entity74Controller(Entity74Service service) { this.service = service; }
    @GetMapping List<Entity74> all() { return service.all(); }
    @GetMapping("/search") List<Entity74> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity74 create(@Valid @RequestBody Entity74 e) { return service.create(e); }
}
