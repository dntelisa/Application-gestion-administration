package es.codeurjc.helloword_vscode.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for handling login-related web requests.
 * Provides endpoints for displaying the login page and handling login errors.
 */
@Controller
public class LoginWebController {
    /* Login page */ 
    @GetMapping("/login")
    public String login() {
        return "login";
    }


    /* Login error page */ 
    @GetMapping("/loginerror")
    public String loginerror() {
        return "loginerror";
    }
}
