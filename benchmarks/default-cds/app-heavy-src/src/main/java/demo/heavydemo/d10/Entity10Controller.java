package demo.heavydemo.d10;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity10")
public class Entity10Controller {
    private final Entity10Service service;
    Entity10Controller(Entity10Service service) { this.service = service; }
    @GetMapping List<Entity10> all() { return service.all(); }
    @GetMapping("/search") List<Entity10> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity10 create(@Valid @RequestBody Entity10 e) { return service.create(e); }
}
