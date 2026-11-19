package dev.portfolio.jobtracker;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tells the frontend the public demo login, so it can offer a one-click "Try the demo" button. */
@RestController
@Profile("demo")
class DemoController {

    @GetMapping("/api/demo")
    Map<String, String> demoAccount() {
        return Map.of("email", DemoDataLoader.DEMO_EMAIL, "password", DemoDataLoader.DEMO_PASSWORD);
    }
}
