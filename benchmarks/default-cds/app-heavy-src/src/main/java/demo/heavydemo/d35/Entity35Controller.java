package demo.heavydemo.d35;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity35")
public class Entity35Controller {
    private final Entity35Service service;
    Entity35Controller(Entity35Service service) { this.service = service; }
    @GetMapping List<Entity35> all() { return service.all(); }
    @GetMapping("/search") List<Entity35> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity35 create(@Valid @RequestBody Entity35 e) { return service.create(e); }
}
