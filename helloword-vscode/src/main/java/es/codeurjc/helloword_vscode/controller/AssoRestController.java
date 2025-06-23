package es.codeurjc.helloword_vscode.controller;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import es.codeurjc.helloword_vscode.dto.AssociationBasicDTO;
import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.dto.PagedResponseDTO;
import es.codeurjc.helloword_vscode.service.AssociationService;

/**
 * REST controller for managing associations.
 * Provides endpoints to create, read, update, and delete associations,
 * as well as manage their images.
 */
@RestController
@RequestMapping("/api/associations")
public class AssoRestController {

    @Autowired
    private AssociationService associationService;

    // GET all associations
    @GetMapping("/")
    public PagedResponseDTO<AssociationBasicDTO> getAllAssociations(Pageable pageable) {
        return associationService.getPagedAssociations(pageable);
    }

    // @GetMapping("/")
    // public Collection<AssociationDTO> getAllAssociations() {
    //     return associationService.findAllDTOs();
    // }

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

    // GET association with image
	@GetMapping("/{id}/image")
	public ResponseEntity<Object> getAssociationImage(@PathVariable long id) throws SQLException, IOException {

		Resource associationImage = associationService.getAssociationImage(id);

		return ResponseEntity
				.ok()
				.header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
				.body(associationImage);

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

    // POST add image to an association
    @PostMapping("/{id}/image")
    public ResponseEntity<Object> createAssoImage(
        @PathVariable long id, @RequestParam MultipartFile imageFile) throws IOException {

        URI location = fromCurrentRequest().build().toUri();
        associationService.createAssoImage(id, location, imageFile.getInputStream(), imageFile.getSize());
        return ResponseEntity.created(location).build();

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

    // PUT update association with image
    @PutMapping("/{id}/image")
        public ResponseEntity<Object> replacePostImage(
        @PathVariable long id, @RequestParam MultipartFile imageFile) throws IOException {
        associationService.replaceAssociationImage(id, imageFile.getInputStream(), imageFile.getSize());
        return ResponseEntity.noContent().build();
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

    // DELETE association with image
	@DeleteMapping("/{id}/image")
	public ResponseEntity<Object> deleteAssociationImage(@PathVariable long id) throws IOException {

		associationService.deleteAssociationImage(id);

		return ResponseEntity.noContent().build();
	}
}

