package demo.heavydemo.d40;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity40")
public class Entity40Controller {
    private final Entity40Service service;
    Entity40Controller(Entity40Service service) { this.service = service; }
    @GetMapping List<Entity40> all() { return service.all(); }
    @GetMapping("/search") List<Entity40> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity40 create(@Valid @RequestBody Entity40 e) { return service.create(e); }
}
