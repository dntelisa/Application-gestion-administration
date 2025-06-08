package es.codeurjc.helloword_vscode.controller;

import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.dto.MemberDTO;
import es.codeurjc.helloword_vscode.dto.MemberTypeDTO;
import es.codeurjc.helloword_vscode.dto.MemberTypeLightDTO;
import es.codeurjc.helloword_vscode.dto.MinuteDTO;
import es.codeurjc.helloword_vscode.dto.MinuteLightDTO;
import es.codeurjc.helloword_vscode.dto.NewAssoRequestDTO;
import es.codeurjc.helloword_vscode.service.AssociationService;
import es.codeurjc.helloword_vscode.service.MemberService;
import es.codeurjc.helloword_vscode.service.MemberTypeService;
import jakarta.servlet.http.HttpServletRequest;



@Controller
public class AssoWebController {

    // Service for database interaction 

    @Autowired
    private MemberService memberService;

    @Autowired
	private AssociationService associationService;

    @Autowired
    private MemberTypeService memberTypeService;


    /* Adds authentication attributes to all templates */ 
    @ModelAttribute
    public void addAttributes(Model model, HttpServletRequest request) {

        // Retrieve the current authentication information
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Determine if the user is authenticated and not anonymous
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());
        model.addAttribute("isAuthenticated", isAuthenticated);
        
        // If authenticated, add the username to the model
        if (isAuthenticated && auth.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            model.addAttribute("username", userDetails.getUsername());
        }

        // Attributes are created based on the user
        Principal principal = request.getUserPrincipal();
        if(principal != null) {
		
			model.addAttribute("logged", true);		
			model.addAttribute("userName", principal.getName());

            // Check if the user has the ADMIN role and add this information to the model
			model.addAttribute("isAdmin", request.isUserInRole("ADMIN"));
			
		} else {
			model.addAttribute("logged", false);
		}
        
    }


    /* Home page: Displays associations */
    @GetMapping("/")
    public String getPosts(Model model, HttpServletRequest request) {
        // Fetch all associations and add them to the model
        model.addAttribute("associations", associationService.findAllDTOs());
        return "index";
    }


    @GetMapping("/association/{id}")
    public String associationId(@PathVariable long id, Model model, Principal principal, HttpServletRequest request) {
        String username = (principal != null) ? principal.getName() : null;
        boolean isAdmin = request.isUserInRole("ADMIN");

        try {
            AssociationDTO association = associationService.getDetailedAssociationDTO(id);

            model.addAttribute("association", association);
            model.addAttribute("minutes", association.minutes());
            model.addAttribute("hasImage", association.image());
            model.addAttribute("isAdmin", isAdmin);

            List<Map<String, Object>> memberTypeData = association.memberTypes().stream().map(mt -> {
                Map<String, Object> data = new HashMap<>();
                data.put("id", mt.id());
                data.put("name", mt.name());
                data.put("member", mt.member());
                data.put("presidentSelected", "president".equalsIgnoreCase(mt.name()));
                data.put("vicePresidentSelected", "vice-president".equalsIgnoreCase(mt.name()));
                data.put("secretarySelected", "secretary".equalsIgnoreCase(mt.name()));
                data.put("treasurerSelected", "treasurer".equalsIgnoreCase(mt.name()));
                data.put("memberSelected", "member".equalsIgnoreCase(mt.name()));
                return data;
            }).collect(Collectors.toList());

            model.addAttribute("memberTypes", memberTypeData);

            boolean isMember = username != null && association.memberTypes().stream()
                .anyMatch(mt -> mt.member().name().equals(username));

            boolean isPresident = false;
            try {
                MemberDTO president = memberTypeService.getPresidentDTO(association);
                isPresident = president != null && president.name().equals(username);
            } catch (IllegalStateException e) {
                isPresident = false;
            }


            model.addAttribute("isMember", isMember);
            model.addAttribute("isPresident", isPresident);

            return "association_detail";

        } catch (ResourceNotFoundException e) {
            return "asso_not_found";
        }
    }




    /* Allows an user to join an association */ 
    @PostMapping("/association/{id}/join")
    public String joinAssociation(@PathVariable Long id, Principal principal) {
        if (principal != null) {
            String username = principal.getName();
            associationService.addUserToAssociation(id, username);
        }
        return "redirect:/association/" + id;
    }


    /* Allow an user to leave an association */ 
    @PostMapping("/association/{id}/leave")
    public String leaveAssociation(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal != null) {
            try {
                MemberDTO userDTO = memberService.findByNameDTO(principal.getName());
                associationService.deleteUserFromAssociationDTO(id, userDTO.id());
            } catch (IllegalStateException e) {
                redirectAttributes.addFlashAttribute("leaveError", e.getMessage());
                return "redirect:/association/" + id;
            }
        }
        return "redirect:/association/" + id;
    }



    /*  Delete association (only for admins) */
    @PostMapping("/association/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
	public String deleteAssociation(@PathVariable long id, Authentication auth) {
        associationService.findByIdDTO(id); // throws if not found
        associationService.deleteAssociation(id); // Delete the association by ID
        return "redirect:/";
	}


    /* Page with form to create association (only for admins) */ 
    @GetMapping("/createasso")
    public String createAsso(){
        return "new_asso";
    }


    /* Create a new association (only for admins) */ 
    @PostMapping("/association/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createAssociation(Model model, NewAssoRequestDTO newAssoRequestDTO) throws IOException, SQLException {

        AssociationDTO createdAsso = createOrReplaceAssociation(null, newAssoRequestDTO, null);
        return "redirect:/association/" + createdAsso.id();
    }

    /* Download Image on association */ 
    @GetMapping("/association/{id}/image")
    public ResponseEntity<Object> downloadImage(@PathVariable long id) throws SQLException, IOException {
    Resource image = associationService.getImage(id);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, "image/png")
        .body(image);
    }

    /* Page to edit association (only for admins) */ 
	@GetMapping("/editasso/{id}")
    @PreAuthorize("hasRole('ADMIN')")
	public String editAsso(Model model, @PathVariable long id) {
        AssociationDTO association = associationService.findByIdDTO(id); // Retrieve the association by ID
        model.addAttribute("association", association);  // Add the association to the model
		return "editAssoPage";
	}
    

    /* Edit association (only for admins) */ 
    @PostMapping("/editasso")
    public String editAssoProcess(Model model, Long id,
                                    NewAssoRequestDTO newAssoRequestDTO,
                                    Boolean removeImage) throws IOException, SQLException {

        AssociationDTO updatedAsso = createOrReplaceAssociation(id, newAssoRequestDTO, removeImage);
        return "redirect:/association/" + updatedAsso.id();
    }


    private AssociationDTO createOrReplaceAssociation(Long id,
                                                    NewAssoRequestDTO request,
                                                    Boolean removeImage) throws IOException, SQLException {
        boolean image = false;
        if (id != null) {
            AssociationDTO old = associationService.findByIdDTO(id);
            image = (removeImage != null && removeImage) ? false : old.image();
        }

        List<MinuteLightDTO> minutes = Collections.emptyList();
        List<MemberTypeLightDTO> memberTypes = Collections.emptyList();

        AssociationDTO dto = new AssociationDTO(id, request.name(), image, memberTypes, minutes);
        AssociationDTO saved = associationService.createOrReplaceAssociation(id, dto);

        MultipartFile imageField = request.imageField();
        if (!imageField.isEmpty()) {
            associationService.createAssociationImage(saved.id(), imageField.getInputStream(), imageField.getSize());
        }

        return saved;
    }


    /* Delete image from association */ 
    @PostMapping("/association/{id}/deleteImage")
    public String deleteAssociationImage(@PathVariable long id) {
        associationService.deleteImage(id);
        return "redirect:/editasso/" + id;
    }
}
