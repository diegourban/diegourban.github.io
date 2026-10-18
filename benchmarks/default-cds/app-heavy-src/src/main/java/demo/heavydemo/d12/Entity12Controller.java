package demo.heavydemo.d12;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity12")
public class Entity12Controller {
    private final Entity12Service service;
    Entity12Controller(Entity12Service service) { this.service = service; }
    @GetMapping List<Entity12> all() { return service.all(); }
    @GetMapping("/search") List<Entity12> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity12 create(@Valid @RequestBody Entity12 e) { return service.create(e); }
}
