package demo.heavydemo.d17;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity17")
public class Entity17Controller {
    private final Entity17Service service;
    Entity17Controller(Entity17Service service) { this.service = service; }
    @GetMapping List<Entity17> all() { return service.all(); }
    @GetMapping("/search") List<Entity17> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity17 create(@Valid @RequestBody Entity17 e) { return service.create(e); }
}
