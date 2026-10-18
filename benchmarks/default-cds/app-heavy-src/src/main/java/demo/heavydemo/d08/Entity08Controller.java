package demo.heavydemo.d08;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity08")
public class Entity08Controller {
    private final Entity08Service service;
    Entity08Controller(Entity08Service service) { this.service = service; }
    @GetMapping List<Entity08> all() { return service.all(); }
    @GetMapping("/search") List<Entity08> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity08 create(@Valid @RequestBody Entity08 e) { return service.create(e); }
}
