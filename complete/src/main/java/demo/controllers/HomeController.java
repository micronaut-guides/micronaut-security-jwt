package demo.controllers;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.Secured;
import java.security.Principal;

@Secured("isAuthenticated()") // <1>
@Controller("/")  // <2>
public class HomeController {

    @Get("/")  // <3>
    String index(Principal principal) {  // <4>
        return principal.getName();
    }
}
