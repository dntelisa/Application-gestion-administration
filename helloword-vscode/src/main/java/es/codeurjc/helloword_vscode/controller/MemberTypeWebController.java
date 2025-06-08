package es.codeurjc.helloword_vscode.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import es.codeurjc.helloword_vscode.service.MemberTypeService;


@Controller
public class MemberTypeWebController {

    @Autowired
    private MemberTypeService memberTypeService;


    /* Adds authentication attributes to all templates */ 
    @ModelAttribute
    public void addAttributes(Model model, HttpServletRequest request) {
        // Retrieve the current authentication information        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());
        
        // Determine if the user is authenticated and not anonymous
        model.addAttribute("isAuthenticated", isAuthenticated);
        
        // Determine if the user is admin
        model.addAttribute("isAdmin", request.isUserInRole("ADMIN"));

        // If authenticated, add the username to the model
        if (isAuthenticated && auth.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            model.addAttribute("username", userDetails.getUsername());
        }
    }


    @PostMapping("/association/{id}/changeRole")
    public String changeMemberRole(
        @PathVariable Long id,
        @RequestParam Long memberTypeId,
        @RequestParam String newRole,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {
        try {
            memberTypeService.changeMemberRole(id, principal.getName(), memberTypeId, newRole);
            redirectAttributes.addFlashAttribute("success", "Role updated successfully.");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("roleChangeError", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("roleChangeError", "An unexpected error occurred.");
        }

        return "redirect:/association/" + id;
    }



    
}
