package demo.heavydemo.d19;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity19")
public class Entity19Controller {
    private final Entity19Service service;
    Entity19Controller(Entity19Service service) { this.service = service; }
    @GetMapping List<Entity19> all() { return service.all(); }
    @GetMapping("/search") List<Entity19> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity19 create(@Valid @RequestBody Entity19 e) { return service.create(e); }
}
