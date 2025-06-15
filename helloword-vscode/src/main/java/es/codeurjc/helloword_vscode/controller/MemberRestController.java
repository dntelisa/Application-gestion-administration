package es.codeurjc.helloword_vscode.controller;

import java.net.URI;
import java.sql.SQLException;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.web.exchanges.HttpExchange.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import es.codeurjc.helloword_vscode.dto.MemberDTO;
import es.codeurjc.helloword_vscode.dto.MemberDetailsDTO;
import es.codeurjc.helloword_vscode.dto.NewMemberRequestDTO;
import es.codeurjc.helloword_vscode.service.MemberService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;


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
    public ResponseEntity<MemberDTO> deleteMember(
            @PathVariable long id,
            Authentication authentication) throws Exception {
        try {
            MemberDTO deleted = memberService.deleteMemberDTO(id, authentication);
            return ResponseEntity.ok(deleted);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    

    // PUT update member
    @PutMapping("/{id}")
    public ResponseEntity<MemberDTO> updateMember(
            @PathVariable long id,
            @RequestBody NewMemberRequestDTO updatedDTO,
            Authentication authentication) {
        try {
            MemberDTO updated = memberService.updateUserIdDTO(id, updatedDTO, authentication);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }



    // POST create new member
    @PostMapping("/")
    public ResponseEntity<MemberDTO> createMember(@RequestBody NewMemberRequestDTO memberDTO) {
        MemberDTO created = memberService.createMember(memberDTO);
        System.out.println("Created ID: " + created.id());
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
