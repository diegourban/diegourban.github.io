package demo.heavydemo.d64;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity64")
public class Entity64Controller {
    private final Entity64Service service;
    Entity64Controller(Entity64Service service) { this.service = service; }
    @GetMapping List<Entity64> all() { return service.all(); }
    @GetMapping("/search") List<Entity64> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity64 create(@Valid @RequestBody Entity64 e) { return service.create(e); }
}
