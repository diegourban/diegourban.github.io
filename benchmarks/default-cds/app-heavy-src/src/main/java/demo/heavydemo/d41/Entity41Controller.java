package demo.heavydemo.d41;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity41")
public class Entity41Controller {
    private final Entity41Service service;
    Entity41Controller(Entity41Service service) { this.service = service; }
    @GetMapping List<Entity41> all() { return service.all(); }
    @GetMapping("/search") List<Entity41> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity41 create(@Valid @RequestBody Entity41 e) { return service.create(e); }
}
