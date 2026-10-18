package demo.heavydemo.d75;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity75")
public class Entity75Controller {
    private final Entity75Service service;
    Entity75Controller(Entity75Service service) { this.service = service; }
    @GetMapping List<Entity75> all() { return service.all(); }
    @GetMapping("/search") List<Entity75> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity75 create(@Valid @RequestBody Entity75 e) { return service.create(e); }
}
