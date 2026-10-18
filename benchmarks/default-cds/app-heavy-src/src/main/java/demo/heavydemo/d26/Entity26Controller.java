package demo.heavydemo.d26;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity26")
public class Entity26Controller {
    private final Entity26Service service;
    Entity26Controller(Entity26Service service) { this.service = service; }
    @GetMapping List<Entity26> all() { return service.all(); }
    @GetMapping("/search") List<Entity26> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity26 create(@Valid @RequestBody Entity26 e) { return service.create(e); }
}
