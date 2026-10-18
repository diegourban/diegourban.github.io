package demo.heavydemo.d62;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity62")
public class Entity62Controller {
    private final Entity62Service service;
    Entity62Controller(Entity62Service service) { this.service = service; }
    @GetMapping List<Entity62> all() { return service.all(); }
    @GetMapping("/search") List<Entity62> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity62 create(@Valid @RequestBody Entity62 e) { return service.create(e); }
}
