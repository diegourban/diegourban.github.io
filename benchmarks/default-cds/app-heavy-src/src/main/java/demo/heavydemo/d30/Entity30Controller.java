package demo.heavydemo.d30;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity30")
public class Entity30Controller {
    private final Entity30Service service;
    Entity30Controller(Entity30Service service) { this.service = service; }
    @GetMapping List<Entity30> all() { return service.all(); }
    @GetMapping("/search") List<Entity30> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity30 create(@Valid @RequestBody Entity30 e) { return service.create(e); }
}
