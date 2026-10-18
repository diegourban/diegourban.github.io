package demo.heavydemo.d52;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity52")
public class Entity52Controller {
    private final Entity52Service service;
    Entity52Controller(Entity52Service service) { this.service = service; }
    @GetMapping List<Entity52> all() { return service.all(); }
    @GetMapping("/search") List<Entity52> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity52 create(@Valid @RequestBody Entity52 e) { return service.create(e); }
}
