package es.codeurjc.helloword_vscode.controller;

import java.net.URI;
import java.sql.SQLException;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.service.AssociationService;


@RestController
@RequestMapping("/api/associations")
public class AssoRestController {

    @Autowired
    private AssociationService associationService;

    // GET all associations
    @GetMapping("/")
    public Collection<AssociationDTO> getAllAssociations() {
        return associationService.findAllDTOs();
    }

    // GET one association by id
    @GetMapping("/{id}")
    public ResponseEntity<AssociationDTO> getAssociation(@PathVariable long id) {
        try {
            AssociationDTO asso = associationService.findByIdDTO(id);
            return ResponseEntity.ok(asso);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST create new association
    @PostMapping("/")
    public ResponseEntity<AssociationDTO> createAssociation(@RequestBody AssociationDTO associationDTO) {
        AssociationDTO created = associationService.createAsso(associationDTO);
        System.out.println("Created ID: " + created.id());
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }

    // PUT update association
    @PutMapping("/{id}")
    public ResponseEntity<AssociationDTO> updateAssociation(@PathVariable long id,
                                                            @RequestBody AssociationDTO updatedDTO) throws SQLException {
        try {
            AssociationDTO updated = associationService.replaceAssociation(id, updatedDTO);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE association
    @DeleteMapping("/{id}")
    public ResponseEntity<AssociationDTO> deleteAssociation(@PathVariable long id) {
        try {
            AssociationDTO deleted = associationService.deleteAssociation(id);
            return ResponseEntity.ok(deleted);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

