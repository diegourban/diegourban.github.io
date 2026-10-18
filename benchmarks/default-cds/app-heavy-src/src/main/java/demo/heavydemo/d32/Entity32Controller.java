package demo.heavydemo.d32;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity32")
public class Entity32Controller {
    private final Entity32Service service;
    Entity32Controller(Entity32Service service) { this.service = service; }
    @GetMapping List<Entity32> all() { return service.all(); }
    @GetMapping("/search") List<Entity32> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity32 create(@Valid @RequestBody Entity32 e) { return service.create(e); }
}
