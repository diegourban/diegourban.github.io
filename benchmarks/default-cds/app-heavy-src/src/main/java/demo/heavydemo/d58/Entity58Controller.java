package demo.heavydemo.d58;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity58")
public class Entity58Controller {
    private final Entity58Service service;
    Entity58Controller(Entity58Service service) { this.service = service; }
    @GetMapping List<Entity58> all() { return service.all(); }
    @GetMapping("/search") List<Entity58> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity58 create(@Valid @RequestBody Entity58 e) { return service.create(e); }
}
