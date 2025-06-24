package es.codeurjc.helloword_vscode.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.dto.MemberDTO;
import es.codeurjc.helloword_vscode.service.AssociationService;
import es.codeurjc.helloword_vscode.service.MemberService;
import es.codeurjc.helloword_vscode.service.MemberTypeService;

/*
 * Controller for managing member types in associations.
 * Provides endpoints to change the role of a member in an association.
 */
@Controller
public class MemberTypeWebController {

    @Autowired
    private MemberTypeService memberTypeService;

    @Autowired
    private AssociationService associationService;

    @Autowired
    private MemberService memberService;

    /* Process to change the member type of someone who part of an association */
    @PostMapping("/association/{id}/changeRole")
    public String changeMemberRole(
        @PathVariable Long id,
        @RequestParam Long memberTypeId,
        @RequestParam String newRole,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {
        try {
            // Retrieve DTOs
            MemberDTO requesterDTO = memberService.findByNameDTO(principal.getName());
            AssociationDTO associationDTO = associationService.findByIdDTO(id);

            // Call service
            memberTypeService.changeMemberRole(associationDTO, requesterDTO, memberTypeId, newRole);

            redirectAttributes.addFlashAttribute("success", "Role updated successfully.");

        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("roleChangeError", e.getMessage());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("roleChangeError", "An unexpected error occurred.");
        }

        return "redirect:/association/" + id;
    }




    
}
