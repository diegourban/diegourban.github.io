package demo.heavydemo.d68;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity68")
public class Entity68Controller {
    private final Entity68Service service;
    Entity68Controller(Entity68Service service) { this.service = service; }
    @GetMapping List<Entity68> all() { return service.all(); }
    @GetMapping("/search") List<Entity68> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity68 create(@Valid @RequestBody Entity68 e) { return service.create(e); }
}
