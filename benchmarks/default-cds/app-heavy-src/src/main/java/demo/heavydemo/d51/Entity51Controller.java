package demo.heavydemo.d51;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity51")
public class Entity51Controller {
    private final Entity51Service service;
    Entity51Controller(Entity51Service service) { this.service = service; }
    @GetMapping List<Entity51> all() { return service.all(); }
    @GetMapping("/search") List<Entity51> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity51 create(@Valid @RequestBody Entity51 e) { return service.create(e); }
}
