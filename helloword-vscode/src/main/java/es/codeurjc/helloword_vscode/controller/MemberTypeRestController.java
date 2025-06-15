package es.codeurjc.helloword_vscode.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.Collection;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import es.codeurjc.helloword_vscode.dto.EditMTRequestDTO;


import es.codeurjc.helloword_vscode.dto.MemberTypeDTO;
import es.codeurjc.helloword_vscode.dto.NewMTRequestDTO;
import es.codeurjc.helloword_vscode.service.MemberTypeService;

@RestController
@RequestMapping("/api/memberTypes")
public class MemberTypeRestController {
    
    @Autowired
    private MemberTypeService memberTypeService;

    // GET all member types
    @GetMapping("/")
    public Collection<MemberTypeDTO> getAllMemberTypes() {
        return memberTypeService.findAllMTDTOs();
    }

    // GET one member type by id
    @GetMapping("/{id}")
    public ResponseEntity<MemberTypeDTO> getMemberType(@PathVariable long id) {
        try {
            MemberTypeDTO memberType = memberTypeService.findByIdMTDTO(id);
            return ResponseEntity.ok(memberType);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST create new member type
    @PostMapping("/")
    public ResponseEntity<MemberTypeDTO> createMemberType(@RequestBody NewMTRequestDTO MTRequestDTO) {
        try {
            MemberTypeDTO created = memberTypeService.createMemberType(MTRequestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT update member type
    @PutMapping("/{id}")
    public ResponseEntity<MemberTypeDTO> updateMemberType(@PathVariable long id,
                                                            @RequestBody EditMTRequestDTO updatedDTO) throws SQLException {
        try {
            MemberTypeDTO updated = memberTypeService.updateMTDTO(id, updatedDTO);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE member type
    @DeleteMapping("/{id}")
    public ResponseEntity<MemberTypeDTO> deleteMemberType(@PathVariable long id) {
        try {
            MemberTypeDTO deleted = memberTypeService.deleteMemberType(id);
            return ResponseEntity.ok(deleted);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
