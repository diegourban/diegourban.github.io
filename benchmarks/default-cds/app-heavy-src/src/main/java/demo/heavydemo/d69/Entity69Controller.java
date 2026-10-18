package demo.heavydemo.d69;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity69")
public class Entity69Controller {
    private final Entity69Service service;
    Entity69Controller(Entity69Service service) { this.service = service; }
    @GetMapping List<Entity69> all() { return service.all(); }
    @GetMapping("/search") List<Entity69> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity69 create(@Valid @RequestBody Entity69 e) { return service.create(e); }
}
