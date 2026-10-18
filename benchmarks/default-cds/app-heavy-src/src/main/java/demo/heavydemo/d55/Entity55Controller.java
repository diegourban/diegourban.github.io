package demo.heavydemo.d55;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity55")
public class Entity55Controller {
    private final Entity55Service service;
    Entity55Controller(Entity55Service service) { this.service = service; }
    @GetMapping List<Entity55> all() { return service.all(); }
    @GetMapping("/search") List<Entity55> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity55 create(@Valid @RequestBody Entity55 e) { return service.create(e); }
}
