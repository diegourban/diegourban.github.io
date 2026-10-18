package demo.heavydemo.d49;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity49")
public class Entity49Controller {
    private final Entity49Service service;
    Entity49Controller(Entity49Service service) { this.service = service; }
    @GetMapping List<Entity49> all() { return service.all(); }
    @GetMapping("/search") List<Entity49> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity49 create(@Valid @RequestBody Entity49 e) { return service.create(e); }
}
