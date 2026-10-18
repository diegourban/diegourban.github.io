package demo.heavydemo.d42;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity42")
public class Entity42Controller {
    private final Entity42Service service;
    Entity42Controller(Entity42Service service) { this.service = service; }
    @GetMapping List<Entity42> all() { return service.all(); }
    @GetMapping("/search") List<Entity42> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity42 create(@Valid @RequestBody Entity42 e) { return service.create(e); }
}
