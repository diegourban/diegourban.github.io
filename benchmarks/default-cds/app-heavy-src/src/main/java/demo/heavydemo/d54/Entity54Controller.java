package demo.heavydemo.d54;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity54")
public class Entity54Controller {
    private final Entity54Service service;
    Entity54Controller(Entity54Service service) { this.service = service; }
    @GetMapping List<Entity54> all() { return service.all(); }
    @GetMapping("/search") List<Entity54> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity54 create(@Valid @RequestBody Entity54 e) { return service.create(e); }
}
