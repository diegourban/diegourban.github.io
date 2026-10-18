package demo.heavydemo.d78;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity78")
public class Entity78Controller {
    private final Entity78Service service;
    Entity78Controller(Entity78Service service) { this.service = service; }
    @GetMapping List<Entity78> all() { return service.all(); }
    @GetMapping("/search") List<Entity78> search(@RequestParam String q) { return service.search(q); }
    @PostMapping Entity78 create(@Valid @RequestBody Entity78 e) { return service.create(e); }
}
