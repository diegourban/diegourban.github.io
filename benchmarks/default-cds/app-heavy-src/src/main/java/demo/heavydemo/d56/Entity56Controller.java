package demo.heavydemo.d56;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity56")
public class Entity56Controller {
    private final Entity56Service service;
    Entity56Controller(Entity56Service service) { this.service = service; }
    @GetMapping List<Entity56> all() { return service.all(); }
    @GetMapping("/search") List<Entity56> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity56 create(@Valid @RequestBody Entity56 e) { return service.create(e); }
}
