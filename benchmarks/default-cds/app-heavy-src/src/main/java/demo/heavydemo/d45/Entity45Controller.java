package demo.heavydemo.d45;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity45")
public class Entity45Controller {
    private final Entity45Service service;
    Entity45Controller(Entity45Service service) { this.service = service; }
    @GetMapping List<Entity45> all() { return service.all(); }
    @GetMapping("/search") List<Entity45> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity45 create(@Valid @RequestBody Entity45 e) { return service.create(e); }
}
