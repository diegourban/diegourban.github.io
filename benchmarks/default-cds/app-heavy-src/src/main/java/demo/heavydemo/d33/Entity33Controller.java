package demo.heavydemo.d33;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity33")
public class Entity33Controller {
    private final Entity33Service service;
    Entity33Controller(Entity33Service service) { this.service = service; }
    @GetMapping List<Entity33> all() { return service.all(); }
    @GetMapping("/search") List<Entity33> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity33 create(@Valid @RequestBody Entity33 e) { return service.create(e); }
}
