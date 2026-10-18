package demo.heavydemo.d14;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity14")
public class Entity14Controller {
    private final Entity14Service service;
    Entity14Controller(Entity14Service service) { this.service = service; }
    @GetMapping List<Entity14> all() { return service.all(); }
    @GetMapping("/search") List<Entity14> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity14 create(@Valid @RequestBody Entity14 e) { return service.create(e); }
}
