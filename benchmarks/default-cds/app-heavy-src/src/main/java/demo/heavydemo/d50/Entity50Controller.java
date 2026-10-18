package demo.heavydemo.d50;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity50")
public class Entity50Controller {
    private final Entity50Service service;
    Entity50Controller(Entity50Service service) { this.service = service; }
    @GetMapping List<Entity50> all() { return service.all(); }
    @GetMapping("/search") List<Entity50> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity50 create(@Valid @RequestBody Entity50 e) { return service.create(e); }
}
