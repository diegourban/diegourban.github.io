package demo.heavydemo.d13;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity13")
public class Entity13Controller {
    private final Entity13Service service;
    Entity13Controller(Entity13Service service) { this.service = service; }
    @GetMapping List<Entity13> all() { return service.all(); }
    @GetMapping("/search") List<Entity13> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity13 create(@Valid @RequestBody Entity13 e) { return service.create(e); }
}
