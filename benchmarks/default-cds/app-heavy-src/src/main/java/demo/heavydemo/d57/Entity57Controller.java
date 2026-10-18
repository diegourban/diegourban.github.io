package demo.heavydemo.d57;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity57")
public class Entity57Controller {
    private final Entity57Service service;
    Entity57Controller(Entity57Service service) { this.service = service; }
    @GetMapping List<Entity57> all() { return service.all(); }
    @GetMapping("/search") List<Entity57> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity57 create(@Valid @RequestBody Entity57 e) { return service.create(e); }
}
