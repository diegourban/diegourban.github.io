package demo.heavydemo.d39;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity39")
public class Entity39Controller {
    private final Entity39Service service;
    Entity39Controller(Entity39Service service) { this.service = service; }
    @GetMapping List<Entity39> all() { return service.all(); }
    @GetMapping("/search") List<Entity39> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity39 create(@Valid @RequestBody Entity39 e) { return service.create(e); }
}
