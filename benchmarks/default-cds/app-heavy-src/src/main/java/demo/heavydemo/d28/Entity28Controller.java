package demo.heavydemo.d28;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity28")
public class Entity28Controller {
    private final Entity28Service service;
    Entity28Controller(Entity28Service service) { this.service = service; }
    @GetMapping List<Entity28> all() { return service.all(); }
    @GetMapping("/search") List<Entity28> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity28 create(@Valid @RequestBody Entity28 e) { return service.create(e); }
}
