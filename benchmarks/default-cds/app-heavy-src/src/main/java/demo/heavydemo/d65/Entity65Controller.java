package demo.heavydemo.d65;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity65")
public class Entity65Controller {
    private final Entity65Service service;
    Entity65Controller(Entity65Service service) { this.service = service; }
    @GetMapping List<Entity65> all() { return service.all(); }
    @GetMapping("/search") List<Entity65> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity65 create(@Valid @RequestBody Entity65 e) { return service.create(e); }
}
