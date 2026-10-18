package demo.heavydemo.d71;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity71")
public class Entity71Controller {
    private final Entity71Service service;
    Entity71Controller(Entity71Service service) { this.service = service; }
    @GetMapping List<Entity71> all() { return service.all(); }
    @GetMapping("/search") List<Entity71> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity71 create(@Valid @RequestBody Entity71 e) { return service.create(e); }
}
