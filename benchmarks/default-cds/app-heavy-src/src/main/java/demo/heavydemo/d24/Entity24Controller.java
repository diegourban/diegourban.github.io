package demo.heavydemo.d24;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity24")
public class Entity24Controller {
    private final Entity24Service service;
    Entity24Controller(Entity24Service service) { this.service = service; }
    @GetMapping List<Entity24> all() { return service.all(); }
    @GetMapping("/search") List<Entity24> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity24 create(@Valid @RequestBody Entity24 e) { return service.create(e); }
}
