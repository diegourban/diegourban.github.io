package demo.heavydemo.d25;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity25")
public class Entity25Controller {
    private final Entity25Service service;
    Entity25Controller(Entity25Service service) { this.service = service; }
    @GetMapping List<Entity25> all() { return service.all(); }
    @GetMapping("/search") List<Entity25> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity25 create(@Valid @RequestBody Entity25 e) { return service.create(e); }
}
