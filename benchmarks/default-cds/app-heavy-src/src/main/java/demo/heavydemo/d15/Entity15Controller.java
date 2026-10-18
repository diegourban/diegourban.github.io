package demo.heavydemo.d15;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity15")
public class Entity15Controller {
    private final Entity15Service service;
    Entity15Controller(Entity15Service service) { this.service = service; }
    @GetMapping List<Entity15> all() { return service.all(); }
    @GetMapping("/search") List<Entity15> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity15 create(@Valid @RequestBody Entity15 e) { return service.create(e); }
}
