package demo.heavydemo.d80;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity80")
public class Entity80Controller {
    private final Entity80Service service;
    Entity80Controller(Entity80Service service) { this.service = service; }
    @GetMapping List<Entity80> all() { return service.all(); }
    @GetMapping("/search") List<Entity80> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity80 create(@Valid @RequestBody Entity80 e) { return service.create(e); }
}
