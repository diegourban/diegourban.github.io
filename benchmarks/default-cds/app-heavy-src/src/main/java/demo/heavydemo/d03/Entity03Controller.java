package demo.heavydemo.d03;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity03")
public class Entity03Controller {
    private final Entity03Service service;
    Entity03Controller(Entity03Service service) { this.service = service; }
    @GetMapping List<Entity03> all() { return service.all(); }
    @GetMapping("/search") List<Entity03> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity03 create(@Valid @RequestBody Entity03 e) { return service.create(e); }
}
