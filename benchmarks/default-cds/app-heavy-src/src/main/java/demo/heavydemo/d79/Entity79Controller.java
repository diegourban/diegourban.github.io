package demo.heavydemo.d79;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity79")
public class Entity79Controller {
    private final Entity79Service service;
    Entity79Controller(Entity79Service service) { this.service = service; }
    @GetMapping List<Entity79> all() { return service.all(); }
    @GetMapping("/search") List<Entity79> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity79 create(@Valid @RequestBody Entity79 e) { return service.create(e); }
}
