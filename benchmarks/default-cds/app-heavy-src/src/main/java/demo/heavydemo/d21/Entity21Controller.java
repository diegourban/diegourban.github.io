package demo.heavydemo.d21;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity21")
public class Entity21Controller {
    private final Entity21Service service;
    Entity21Controller(Entity21Service service) { this.service = service; }
    @GetMapping List<Entity21> all() { return service.all(); }
    @GetMapping("/search") List<Entity21> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity21 create(@Valid @RequestBody Entity21 e) { return service.create(e); }
}
