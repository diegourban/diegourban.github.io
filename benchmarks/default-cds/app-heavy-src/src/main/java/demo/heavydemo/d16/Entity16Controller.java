package demo.heavydemo.d16;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity16")
public class Entity16Controller {
    private final Entity16Service service;
    Entity16Controller(Entity16Service service) { this.service = service; }
    @GetMapping List<Entity16> all() { return service.all(); }
    @GetMapping("/search") List<Entity16> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity16 create(@Valid @RequestBody Entity16 e) { return service.create(e); }
}
