package demo.heavydemo.d23;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity23")
public class Entity23Controller {
    private final Entity23Service service;
    Entity23Controller(Entity23Service service) { this.service = service; }
    @GetMapping List<Entity23> all() { return service.all(); }
    @GetMapping("/search") List<Entity23> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity23 create(@Valid @RequestBody Entity23 e) { return service.create(e); }
}
