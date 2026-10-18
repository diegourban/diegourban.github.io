package demo.heavydemo.d60;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity60")
public class Entity60Controller {
    private final Entity60Service service;
    Entity60Controller(Entity60Service service) { this.service = service; }
    @GetMapping List<Entity60> all() { return service.all(); }
    @GetMapping("/search") List<Entity60> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity60 create(@Valid @RequestBody Entity60 e) { return service.create(e); }
}
