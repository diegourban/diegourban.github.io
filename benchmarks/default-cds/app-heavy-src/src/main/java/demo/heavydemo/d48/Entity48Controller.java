package demo.heavydemo.d48;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity48")
public class Entity48Controller {
    private final Entity48Service service;
    Entity48Controller(Entity48Service service) { this.service = service; }
    @GetMapping List<Entity48> all() { return service.all(); }
    @GetMapping("/search") List<Entity48> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity48 create(@Valid @RequestBody Entity48 e) { return service.create(e); }
}
