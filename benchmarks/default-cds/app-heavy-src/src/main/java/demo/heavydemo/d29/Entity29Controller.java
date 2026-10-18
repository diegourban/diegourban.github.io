package demo.heavydemo.d29;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity29")
public class Entity29Controller {
    private final Entity29Service service;
    Entity29Controller(Entity29Service service) { this.service = service; }
    @GetMapping List<Entity29> all() { return service.all(); }
    @GetMapping("/search") List<Entity29> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity29 create(@Valid @RequestBody Entity29 e) { return service.create(e); }
}
