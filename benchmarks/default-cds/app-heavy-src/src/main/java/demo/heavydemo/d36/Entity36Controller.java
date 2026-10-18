package demo.heavydemo.d36;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity36")
public class Entity36Controller {
    private final Entity36Service service;
    Entity36Controller(Entity36Service service) { this.service = service; }
    @GetMapping List<Entity36> all() { return service.all(); }
    @GetMapping("/search") List<Entity36> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity36 create(@Valid @RequestBody Entity36 e) { return service.create(e); }
}
