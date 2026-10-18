package demo.heavydemo.d01;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity01")
public class Entity01Controller {
    private final Entity01Service service;
    Entity01Controller(Entity01Service service) { this.service = service; }
    @GetMapping List<Entity01> all() { return service.all(); }
    @GetMapping("/search") List<Entity01> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity01 create(@Valid @RequestBody Entity01 e) { return service.create(e); }
}
