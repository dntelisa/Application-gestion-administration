package es.codeurjc.helloword_vscode.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.dto.MinuteDTO;
import es.codeurjc.helloword_vscode.dto.NewMinuteRequestDTO;
import es.codeurjc.helloword_vscode.model.Association;
import es.codeurjc.helloword_vscode.service.AssociationService;
import es.codeurjc.helloword_vscode.service.MemberService;
import es.codeurjc.helloword_vscode.service.MinuteService;

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
        // Optional<Association> association = associationService.findById(id);
        // Add association and members to the model
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
        //model.addAttribute("members", minuteService.findMembers(association.get()));
        //model.addAttribute("participants", minuteService.findParticipants(minute));
        //model.addAttribute("noPart", minuteService.findNoParticipants(association.get(), minute));

        return "editMinutePage";
		/*if (association.isPresent()) {
			return "editMinutePage";
		} else {
			return "redirect:/";
		}*/
	}


    /* Edit minute process */
    @PostMapping("/editminute")
    public String editMinuteProcess(@RequestParam long minuteId, 
                                    @RequestParam long assoId,
                                    @RequestParam String date,
                                    @RequestParam(required = false) List<Long> participantsIds, 
                                    @RequestParam String content, 
                                    @RequestParam double duration, 
                                    Model model,
                                    RedirectAttributes redirectAttributes
                                    ) throws IOException {
        // Check if participants are selected                                
        if (participantsIds == null || participantsIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "You need to select at least one participant");
            return "redirect:/minute/" + minuteId + "/asso/" + assoId + "/edit";
        }
        
        // Retrieve the minute and association by their IDs
        //Minute minute = minuteService.findById(minuteId).orElseThrow();
        MinuteDTO minuteDTO = minuteService.findByIdDTO(minuteId);
        Optional<Association> association = associationService.findById(assoId);
        associationService.findByIdDTO(assoId);
        if(association.isPresent()){
            // Save the updated minute
            minuteService.updateDTO(minuteDTO, date, participantsIds, content, duration, association.get());
        }
        return "redirect:/association/" + assoId;
    }
}
