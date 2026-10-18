package demo.heavydemo.d66;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity66")
public class Entity66Controller {
    private final Entity66Service service;
    Entity66Controller(Entity66Service service) { this.service = service; }
    @GetMapping List<Entity66> all() { return service.all(); }
    @GetMapping("/search") List<Entity66> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity66 create(@Valid @RequestBody Entity66 e) { return service.create(e); }
}
