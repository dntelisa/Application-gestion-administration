package es.codeurjc.helloword_vscode.controller;

import java.io.IOException;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.dto.EditMinuteRequestDTO;
import es.codeurjc.helloword_vscode.dto.MinuteDTO;
import es.codeurjc.helloword_vscode.dto.NewMinuteRequestDTO;
import es.codeurjc.helloword_vscode.service.AssociationService;
import es.codeurjc.helloword_vscode.service.MemberService;
import es.codeurjc.helloword_vscode.service.MinuteService;

/**
 * Controller for managing minutes in the web application.
 * Provides endpoints for creating, editing, and deleting minutes,
 * as well as displaying the minute creation form.
 */
@Controller
public class MinuteWebController {

    // Service for database interaction 

    @Autowired
	private MinuteService minuteService;

    @Autowired
	private AssociationService associationService;

    @Autowired MemberService memberService;

    /* Create minute */
    @PostMapping("/association/{id}/new_minute")
    public String createMinuteDTO(@PathVariable long id,
                                NewMinuteRequestDTO dto,
                                Model model) throws Exception {
        AssociationDTO assoDTO = associationService.findByIdDTO(id);

        try {
            minuteService.createMinute(assoDTO, dto);
            return "redirect:/association/" + id;
        } catch (IllegalArgumentException e) {
            model.addAttribute("association", assoDTO);
            model.addAttribute("members", memberService.findMembersByAssociationId(id));
            model.addAttribute("error", e.getMessage());
            return "new_minute";
        }
    }
    
    /* Page with forms to create minute (only if you're in association) */
    @PostMapping("/association/{id}/createMinute")
	public String createMinuteDTO(Model model, @PathVariable long id) {
        // Retrieve the association by ID
        AssociationDTO associationDTO = associationService.findByIdDTO(id);

        // Add association and members to the model
        model.addAttribute("association", associationDTO);
        model.addAttribute("members", minuteService.findMembersDTO(associationDTO));
        //model.addAttribute("members", minuteService.findMembersDTO(associationDTO));
        model.addAttribute("today", LocalDate.now());
        return "new_minute"; 
	}

    /* Delete minute */
    @PostMapping("/minute/{minuteId}/asso/{assoId}/delete")
    public String deleteMinute(@PathVariable Long assoId, @PathVariable Long minuteId){
        minuteService.deleteMinuteByIdDTO(minuteId, assoId);
        return "redirect:/association/" + assoId;
    }


    /* Edit minute page */
    @GetMapping("/minute/{minuteId}/asso/{assoId}/edit")
    @PreAuthorize("hasRole('ADMIN')")
	public String editMinute(Model model, @PathVariable Long assoId, @PathVariable Long minuteId) {
		// Retrieve the association and minute by their IDs
        AssociationDTO associationDTO = associationService.findByIdDTO(assoId);
        MinuteDTO minuteDTO = minuteService.findByIdDTO(minuteId);
        
        // Add association, minute, and related data to the model
        model.addAttribute("association", associationDTO);
        model.addAttribute("minute", minuteDTO);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("members", minuteService.findMembersDTO(associationDTO));
        model.addAttribute("participants", minuteService.findParticipantsDTO(minuteDTO));
        model.addAttribute("noPart", minuteService.findNoParticipantsDTO(associationDTO, minuteDTO));

        return "editMinutePage";
	}


    /* Edit minute process */
    @PostMapping("/editminute")
    public String editMinuteProcess(@ModelAttribute EditMinuteRequestDTO dto,
                                    RedirectAttributes redirectAttributes) throws IOException {

        // Validation minimal côté contrôleur
        if (dto.participantsIds() == null || dto.participantsIds().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "You need to select at least one participant");
            return "redirect:/minute/" + dto.minuteId() + "/asso/" + dto.assoId() + "/edit";
        }

        minuteService.updateDTO(dto);

        return "redirect:/association/" + dto.assoId();
    }

}
