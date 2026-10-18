package demo.heavydemo.d59;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity59")
public class Entity59Controller {
    private final Entity59Service service;
    Entity59Controller(Entity59Service service) { this.service = service; }
    @GetMapping List<Entity59> all() { return service.all(); }
    @GetMapping("/search") List<Entity59> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity59 create(@Valid @RequestBody Entity59 e) { return service.create(e); }
}
