package es.codeurjc.helloword_vscode.controller;

import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.dto.EditMinuteRequestDTO;
import es.codeurjc.helloword_vscode.dto.MinuteDTO;
import es.codeurjc.helloword_vscode.dto.NewMinuteRequestDTO;
import es.codeurjc.helloword_vscode.service.AssociationService;
import es.codeurjc.helloword_vscode.service.MinuteService;

/**
 * REST controller for managing minutes in the application.
 * Provides endpoints for creating, retrieving, updating, and deleting minutes.
 */
@RestController
@RequestMapping("/api/minutes")
public class MinuteRestController {

    @Autowired
    private MinuteService minuteService;

    @Autowired
    private AssociationService associationService;

    // GET all minutes
    @GetMapping("/")
    public Page<MinuteDTO> getAllMinutes(Pageable pageable) {
        return minuteService.getAllMinutes(pageable);
    }
    // GET all minutes
    // @GetMapping("/")
    // public ResponseEntity<List<MinuteDTO>> getAllMinutes() {
    //     List<MinuteDTO> minutes = minuteService.findAllDTOs();
    //     return ResponseEntity.ok(minutes);
    // }

    // GET one minute by id
    @GetMapping("/{id}")
    public ResponseEntity<MinuteDTO> getMinute(@PathVariable long id) {
        try {
            MinuteDTO minute = minuteService.findByIdDTO(id);
            return ResponseEntity.ok(minute);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }


    // POST create new minute
    @PostMapping("association/{id}")
    public ResponseEntity<MinuteDTO> createMinute(@PathVariable long id,
                                                @RequestBody NewMinuteRequestDTO dto,
                                                Authentication authentication) {
        try {
            AssociationDTO associationDTO = associationService.findByIdDTO(id);
            MinuteDTO createdMinute = minuteService.createMinute(associationDTO, dto, authentication);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdMinute);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }


    // PUT update minute
    @PutMapping("/{id}")
    public ResponseEntity<MinuteDTO> updateMinute(@PathVariable long id,
                                                            @RequestBody EditMinuteRequestDTO updatedDTO) throws SQLException {
        try {
            MinuteDTO updated = minuteService.updateDTO(id, updatedDTO);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE minute
     @DeleteMapping("/{id}")
    public ResponseEntity<MinuteDTO> deleteMinute(@PathVariable long id) {
        try {
            MinuteDTO deleted = minuteService.deleteMinuteByIdDTORest(id);
            return ResponseEntity.ok(deleted);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
