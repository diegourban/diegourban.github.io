package demo.heavydemo.d11;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity11")
public class Entity11Controller {
    private final Entity11Service service;
    Entity11Controller(Entity11Service service) { this.service = service; }
    @GetMapping List<Entity11> all() { return service.all(); }
    @GetMapping("/search") List<Entity11> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity11 create(@Valid @RequestBody Entity11 e) { return service.create(e); }
}
