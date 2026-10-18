package demo.heavydemo.d22;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity22")
public class Entity22Controller {
    private final Entity22Service service;
    Entity22Controller(Entity22Service service) { this.service = service; }
    @GetMapping List<Entity22> all() { return service.all(); }
    @GetMapping("/search") List<Entity22> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity22 create(@Valid @RequestBody Entity22 e) { return service.create(e); }
}
