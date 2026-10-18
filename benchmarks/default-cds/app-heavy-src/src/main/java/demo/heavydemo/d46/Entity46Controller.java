package demo.heavydemo.d46;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity46")
public class Entity46Controller {
    private final Entity46Service service;
    Entity46Controller(Entity46Service service) { this.service = service; }
    @GetMapping List<Entity46> all() { return service.all(); }
    @GetMapping("/search") List<Entity46> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity46 create(@Valid @RequestBody Entity46 e) { return service.create(e); }
}
