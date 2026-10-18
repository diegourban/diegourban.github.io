package demo.heavydemo.d61;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity61")
public class Entity61Controller {
    private final Entity61Service service;
    Entity61Controller(Entity61Service service) { this.service = service; }
    @GetMapping List<Entity61> all() { return service.all(); }
    @GetMapping("/search") List<Entity61> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity61 create(@Valid @RequestBody Entity61 e) { return service.create(e); }
}
