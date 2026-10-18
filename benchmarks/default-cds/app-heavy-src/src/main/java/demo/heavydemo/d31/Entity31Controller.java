package demo.heavydemo.d31;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity31")
public class Entity31Controller {
    private final Entity31Service service;
    Entity31Controller(Entity31Service service) { this.service = service; }
    @GetMapping List<Entity31> all() { return service.all(); }
    @GetMapping("/search") List<Entity31> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity31 create(@Valid @RequestBody Entity31 e) { return service.create(e); }
}
