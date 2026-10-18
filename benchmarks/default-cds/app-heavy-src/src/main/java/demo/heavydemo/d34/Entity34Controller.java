package demo.heavydemo.d34;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity34")
public class Entity34Controller {
    private final Entity34Service service;
    Entity34Controller(Entity34Service service) { this.service = service; }
    @GetMapping List<Entity34> all() { return service.all(); }
    @GetMapping("/search") List<Entity34> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity34 create(@Valid @RequestBody Entity34 e) { return service.create(e); }
}
