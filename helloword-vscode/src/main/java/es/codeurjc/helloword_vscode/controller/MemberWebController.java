package es.codeurjc.helloword_vscode.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import es.codeurjc.helloword_vscode.service.MemberService;
import es.codeurjc.helloword_vscode.service.AssociationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.dto.MemberDTO;
import es.codeurjc.helloword_vscode.dto.NewMemberRequestDTO;
import es.codeurjc.helloword_vscode.model.Member;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
public class MemberWebController {

    // Service for database interaction 

    @Autowired
    private AssociationService associationService;

    @Autowired
    private MemberService memberService;
    

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
    

    /*  View all members */
    @GetMapping("/members")
    public String showMembers(Model model) {
        // Fetch all users and add them to the model
        model.addAttribute("isMember", memberService.findAllDTOs());
        return "members";
    }

    /* User profile page */ 
    @GetMapping("/profile")
    public String profile(Model model, HttpServletRequest request) {
        // Retrieve the username of the authenticated user
        String name = request.getUserPrincipal().getName();
        Optional<Member> member = memberService.findByName(name);
        
        if(member.isPresent()){
            // Add the username and admin status to the model
            model.addAttribute("username", member.get());
            return "profile";
        } else { 
            return "redirect:/";
        }
    }


    /* Displays details of a specific user */ 
    @GetMapping("/user/{id}")
    public String userId(@PathVariable long id, Model model) {
        Optional<Member> memberOpt = memberService.findById(id);
        if (memberOpt.isPresent()) {
            Member member = memberOpt.get();
            model.addAttribute("memberAttribute", member);
            model.addAttribute("associationRoles", memberService.getAssociationRoles(member));
            model.addAttribute("userMinutes", memberService.getUserMinutes(member));
            return "user_detail";
        } else {
            return "user_not_found";
        }
    }


    /* Research of a specific association or user by ID */
    @GetMapping("/search")
    public String searchUserOrAssociation(@RequestParam(name = "searchId", required = false) Long id,
                                          @RequestParam(name = "searchType", required = false) String searchType,
                                          Model model) {
        if (id != null && "user".equals(searchType)) {
            // Search for a user by ID and add to the model
            memberService.findById(id).ifPresent(user ->
                model.addAttribute("isMember", List.of(user))
            );
            return "members";
        }
    
        if (id != null && "association".equals(searchType)) {
            AssociationDTO associationDTO = associationService.findByIdDTO(id);
            model.addAttribute("assofind", associationDTO);
            return "index";
        }
        return "index";
    }
    

    /* Creation of an user */
    @PostMapping("/login/create")
    public String User(@ModelAttribute NewMemberRequestDTO dto, Model model) {
        try {
            memberService.createMember(dto);
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "new_member";
        }
    }


    /* Page with the form of user creation */
    @GetMapping("/profile/create")
    public String createPage() {
        return "new_member";
    }


    /* Page with the form of user edition */
    @GetMapping("/profile/edit")
    public String editProfile(Model model, Principal principal) {
        MemberDTO userDTO = memberService.findByName(principal.getName())
                                        .map(member -> new MemberDTO(member.getId(), member.getName(), member.getSurname()))
                                        .orElseThrow();
        model.addAttribute("user", userDTO);
        return "edit_profile";
    }


    /* Edition of an user */
    @PostMapping("/profile/update")
    public String updateProfile(Principal principal,
                                @ModelAttribute NewMemberRequestDTO dto,
                                Model model) {
        try {
            memberService.updateUserDTO(principal.getName(), dto);
            model.addAttribute("triggerLogout", true);
            return "post_update_profile";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", dto);  // Pour réafficher le formulaire avec les données
            return "edit_profile";
        }
    }


    /*  Page to confirm deletion of user */
    @GetMapping("/profile/delete")
    @PreAuthorize("isAuthenticated()")
    public String deleteConfirmation() {
        return "confirm_delete";
    }

    
    /* Deletion of our own account */ 
    @PostMapping("/profile/delete/confirm")
    @PreAuthorize("isAuthenticated()")
    public String deleteOwnAccount(Principal principal, HttpServletRequest request) throws IOException {
        String username = principal.getName();
        Optional<Member> member = memberService.findByName(username);

        if (member.isPresent()) {
            // Delete the user by ID
            memberService.delete(member.get());

            // Logout after deletion
            try {
                request.logout();
            } catch (ServletException e) {
                e.printStackTrace();
            }

            return "redirect:/";
        } else {
            return "redirect:/";
        }
    }


    /* Deletion of an user (only for admins) */
    @GetMapping("/profile/{id}/delete")
	public String deleteMember(@PathVariable long id) throws IOException {
            memberService.deleteMemberDTO(id);
			return "redirect:/";
    }

}

