package demo.heavydemo.d20;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity20")
public class Entity20Controller {
    private final Entity20Service service;
    Entity20Controller(Entity20Service service) { this.service = service; }
    @GetMapping List<Entity20> all() { return service.all(); }
    @GetMapping("/search") List<Entity20> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity20 create(@Valid @RequestBody Entity20 e) { return service.create(e); }
}
