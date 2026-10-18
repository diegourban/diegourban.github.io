package demo.heavydemo.d77;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity77")
public class Entity77Controller {
    private final Entity77Service service;
    Entity77Controller(Entity77Service service) { this.service = service; }
    @GetMapping List<Entity77> all() { return service.all(); }
    @GetMapping("/search") List<Entity77> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity77 create(@Valid @RequestBody Entity77 e) { return service.create(e); }
}
