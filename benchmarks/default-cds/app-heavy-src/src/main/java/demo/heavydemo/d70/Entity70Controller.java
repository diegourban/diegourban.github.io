package demo.heavydemo.d70;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity70")
public class Entity70Controller {
    private final Entity70Service service;
    Entity70Controller(Entity70Service service) { this.service = service; }
    @GetMapping List<Entity70> all() { return service.all(); }
    @GetMapping("/search") List<Entity70> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity70 create(@Valid @RequestBody Entity70 e) { return service.create(e); }
}
