package distribution_system.webApp.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DefaultController {
    @RequestMapping("/")
    public String startPage(){
        return "start";
    }
}
