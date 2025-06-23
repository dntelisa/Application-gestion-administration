package es.codeurjc.helloword_vscode.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.dto.MemberDTO;
import es.codeurjc.helloword_vscode.dto.MemberDetailsDTO;
import es.codeurjc.helloword_vscode.dto.NewMemberRequestDTO;
import es.codeurjc.helloword_vscode.model.Member;
import es.codeurjc.helloword_vscode.service.AssociationService;
import es.codeurjc.helloword_vscode.service.MemberService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller for managing members in the web application.
 * Provides endpoints for viewing, creating, updating, and deleting members,
 * as well as searching for members and associations.
 */
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

        // Determine if the user is admin
        model.addAttribute("isAdmin", request.isUserInRole("ADMIN"));

    }
    

    /*  View all members */
    @GetMapping("/members")
    public String memberPage() {
        return "members";
    }
    // @GetMapping("/members")
    // public String showMembers(Model model) {
    //     // Fetch all users and add them to the model
    //     model.addAttribute("isMember", memberService.findAllDTOs());
    //     return "members";
    // }

    /* User profile page */ 
    @GetMapping("/profile")
    public String profile(Model model, HttpServletRequest request) {
        String name = request.getUserPrincipal().getName();

        try {
            MemberDTO member = memberService.findByNameDTO(name);
            MemberDetailsDTO memberDetails = memberService.findDetailsById(member.id());

            model.addAttribute("memberAttribute", memberDetails);
            return "profile";
        } catch (Exception e) {
            return "redirect:/";
        }
    }



    /* Displays details of a specific user */ 
    @GetMapping("/user/{id}")
    public String userId(@PathVariable long id, Model model) {
        try {
            MemberDetailsDTO memberDetails = memberService.findDetailsById(id);
            model.addAttribute("memberAttribute", memberDetails); // Complete DTO
            return "user_detail";
        } catch (ResourceNotFoundException e) {
            return "user_not_found";
        }
    }



    /* Research of a specific association or user by ID */
    @PostMapping("/search")
    public String searchUserOrAssociation(@RequestParam(name = "searchId", required = false) Long id,
                                        @RequestParam(name = "searchType", required = false) String searchType,
                                        Model model) {
        if (id != null && "user".equals(searchType)) {
            try {
                MemberDTO user = memberService.findByIdDTO(id); 
                model.addAttribute("isMember", List.of(user));
                return "members_search";
            } catch (NoSuchElementException | EntityNotFoundException e) {
                return "user_not_found";
            }
        }

        if (id != null && "association".equals(searchType)) {
            try {
                AssociationDTO associationDTO = associationService.findByIdDTO(id);
                model.addAttribute("assofind", associationDTO);
                return "asso_search";
            } catch (NoSuchElementException | EntityNotFoundException e) {
                return "asso_not_found";
            }
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
            model.addAttribute("user", dto);  // To redisplay the form with the data
            return "edit_profile";
        }
    }


    /*  Page to confirm deletion of user */
    @GetMapping("/profile/delete")
    public String deleteConfirmation() {
        return "confirm_delete";
    }

    
    /* Deletion of our own account */ 
    @PostMapping("/profile/delete/confirm")
    public String deleteOwnAccount(Principal principal, HttpServletRequest request) throws IOException {
        String username = principal.getName();

        try {
            Member member = memberService.findByName(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Delete user
            memberService.delete(member);

            // Logout after deletion
            try {
                request.logout();
            } catch (ServletException e) {
                throw new IOException("Logout failed", e);
            }

            return "redirect:/";
        } catch (ResourceNotFoundException e) {
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

