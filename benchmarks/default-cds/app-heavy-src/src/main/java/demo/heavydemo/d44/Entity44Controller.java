package demo.heavydemo.d44;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity44")
public class Entity44Controller {
    private final Entity44Service service;
    Entity44Controller(Entity44Service service) { this.service = service; }
    @GetMapping List<Entity44> all() { return service.all(); }
    @GetMapping("/search") List<Entity44> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity44 create(@Valid @RequestBody Entity44 e) { return service.create(e); }
}
