package demo.heavydemo.d47;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity47")
public class Entity47Controller {
    private final Entity47Service service;
    Entity47Controller(Entity47Service service) { this.service = service; }
    @GetMapping List<Entity47> all() { return service.all(); }
    @GetMapping("/search") List<Entity47> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity47 create(@Valid @RequestBody Entity47 e) { return service.create(e); }
}
