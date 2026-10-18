package demo.heavydemo.d09;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity09")
public class Entity09Controller {
    private final Entity09Service service;
    Entity09Controller(Entity09Service service) { this.service = service; }
    @GetMapping List<Entity09> all() { return service.all(); }
    @GetMapping("/search") List<Entity09> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity09 create(@Valid @RequestBody Entity09 e) { return service.create(e); }
}
