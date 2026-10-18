package demo.heavydemo.d04;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity04")
public class Entity04Controller {
    private final Entity04Service service;
    Entity04Controller(Entity04Service service) { this.service = service; }
    @GetMapping List<Entity04> all() { return service.all(); }
    @GetMapping("/search") List<Entity04> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity04 create(@Valid @RequestBody Entity04 e) { return service.create(e); }
}
