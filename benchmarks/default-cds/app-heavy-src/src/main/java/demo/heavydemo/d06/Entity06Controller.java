package demo.heavydemo.d06;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity06")
public class Entity06Controller {
    private final Entity06Service service;
    Entity06Controller(Entity06Service service) { this.service = service; }
    @GetMapping List<Entity06> all() { return service.all(); }
    @GetMapping("/search") List<Entity06> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity06 create(@Valid @RequestBody Entity06 e) { return service.create(e); }
}
