package demo.heavydemo.d05;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity05")
public class Entity05Controller {
    private final Entity05Service service;
    Entity05Controller(Entity05Service service) { this.service = service; }
    @GetMapping List<Entity05> all() { return service.all(); }
    @GetMapping("/search") List<Entity05> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity05 create(@Valid @RequestBody Entity05 e) { return service.create(e); }
}
