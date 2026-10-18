package demo.heavydemo.d38;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity38")
public class Entity38Controller {
    private final Entity38Service service;
    Entity38Controller(Entity38Service service) { this.service = service; }
    @GetMapping List<Entity38> all() { return service.all(); }
    @GetMapping("/search") List<Entity38> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity38 create(@Valid @RequestBody Entity38 e) { return service.create(e); }
}
