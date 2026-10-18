package demo.heavydemo.d73;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity73")
public class Entity73Controller {
    private final Entity73Service service;
    Entity73Controller(Entity73Service service) { this.service = service; }
    @GetMapping List<Entity73> all() { return service.all(); }
    @GetMapping("/search") List<Entity73> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity73 create(@Valid @RequestBody Entity73 e) { return service.create(e); }
}
