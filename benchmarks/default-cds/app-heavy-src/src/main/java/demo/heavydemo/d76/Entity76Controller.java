package demo.heavydemo.d76;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity76")
public class Entity76Controller {
    private final Entity76Service service;
    Entity76Controller(Entity76Service service) { this.service = service; }
    @GetMapping List<Entity76> all() { return service.all(); }
    @GetMapping("/search") List<Entity76> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity76 create(@Valid @RequestBody Entity76 e) { return service.create(e); }
}
