package demo.heavydemo.d43;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity43")
public class Entity43Controller {
    private final Entity43Service service;
    Entity43Controller(Entity43Service service) { this.service = service; }
    @GetMapping List<Entity43> all() { return service.all(); }
    @GetMapping("/search") List<Entity43> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity43 create(@Valid @RequestBody Entity43 e) { return service.create(e); }
}
