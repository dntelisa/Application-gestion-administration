package es.codeurjc.helloword_vscode.controller;

import java.sql.SQLException;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.dto.MemberDTO;
import es.codeurjc.helloword_vscode.dto.MemberDetailsDTO;
import es.codeurjc.helloword_vscode.dto.NewMemberRequestDTO;
import es.codeurjc.helloword_vscode.service.MemberService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/members")
public class MemberRestController {
    
    @Autowired
    MemberService memberService;

    // GET all members
    @GetMapping("/")
    public Collection<MemberDTO> getAllMembers() {
        return memberService.findAllDTOs();
    }

    // GET Member with id
    @GetMapping("/{id}")
    public ResponseEntity<MemberDetailsDTO> getMemberDetails(@PathVariable Long id) {
        try {
            MemberDetailsDTO dto = memberService.findDetailsById(id);
            return ResponseEntity.ok(dto);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // DELETE member
    @DeleteMapping("/{id}")
    public ResponseEntity<MemberDTO> deleteMember(@PathVariable long id) throws Exception {
        try {
            MemberDTO deleted = memberService.deleteMemberDTO(id);
            return ResponseEntity.ok(deleted);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    

    // PUT update member
    @PutMapping("/{id}")
    public ResponseEntity<MemberDTO> updateMember(@PathVariable long id,
                                                            @RequestBody NewMemberRequestDTO updatedDTO) throws SQLException {
        try {
            MemberDTO updated = memberService.updateUserIdDTO(id, updatedDTO);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
