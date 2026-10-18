package demo.heavydemo.d63;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity63")
public class Entity63Controller {
    private final Entity63Service service;
    Entity63Controller(Entity63Service service) { this.service = service; }
    @GetMapping List<Entity63> all() { return service.all(); }
    @GetMapping("/search") List<Entity63> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity63 create(@Valid @RequestBody Entity63 e) { return service.create(e); }
}
