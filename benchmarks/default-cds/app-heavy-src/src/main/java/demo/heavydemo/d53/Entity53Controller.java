package demo.heavydemo.d53;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity53")
public class Entity53Controller {
    private final Entity53Service service;
    Entity53Controller(Entity53Service service) { this.service = service; }
    @GetMapping List<Entity53> all() { return service.all(); }
    @GetMapping("/search") List<Entity53> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity53 create(@Valid @RequestBody Entity53 e) { return service.create(e); }
}
