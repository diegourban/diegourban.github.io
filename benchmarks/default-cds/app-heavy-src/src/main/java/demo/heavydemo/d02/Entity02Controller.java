package demo.heavydemo.d02;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity02")
public class Entity02Controller {
    private final Entity02Service service;
    Entity02Controller(Entity02Service service) { this.service = service; }
    @GetMapping List<Entity02> all() { return service.all(); }
    @GetMapping("/search") List<Entity02> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity02 create(@Valid @RequestBody Entity02 e) { return service.create(e); }
}
