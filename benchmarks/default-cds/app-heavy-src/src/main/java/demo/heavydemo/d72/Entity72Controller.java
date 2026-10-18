package demo.heavydemo.d72;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity72")
public class Entity72Controller {
    private final Entity72Service service;
    Entity72Controller(Entity72Service service) { this.service = service; }
    @GetMapping List<Entity72> all() { return service.all(); }
    @GetMapping("/search") List<Entity72> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity72 create(@Valid @RequestBody Entity72 e) { return service.create(e); }
}
