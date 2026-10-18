package demo.heavydemo.d07;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity07")
public class Entity07Controller {
    private final Entity07Service service;
    Entity07Controller(Entity07Service service) { this.service = service; }
    @GetMapping List<Entity07> all() { return service.all(); }
    @GetMapping("/search") List<Entity07> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity07 create(@Valid @RequestBody Entity07 e) { return service.create(e); }
}
