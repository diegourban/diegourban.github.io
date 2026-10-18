package demo.heavydemo.d27;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity27")
public class Entity27Controller {
    private final Entity27Service service;
    Entity27Controller(Entity27Service service) { this.service = service; }
    @GetMapping List<Entity27> all() { return service.all(); }
    @GetMapping("/search") List<Entity27> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity27 create(@Valid @RequestBody Entity27 e) { return service.create(e); }
}
