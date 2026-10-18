package demo.heavydemo.d18;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity18")
public class Entity18Controller {
    private final Entity18Service service;
    Entity18Controller(Entity18Service service) { this.service = service; }
    @GetMapping List<Entity18> all() { return service.all(); }
    @GetMapping("/search") List<Entity18> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity18 create(@Valid @RequestBody Entity18 e) { return service.create(e); }
}
