package es.codeurjc.helloword_vscode.controller;

import java.net.URI;
import java.sql.SQLException;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import java.util.List;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.dto.MinuteDTO;
import es.codeurjc.helloword_vscode.dto.NewMinuteRequestDTO;
import es.codeurjc.helloword_vscode.service.AssociationService;
import es.codeurjc.helloword_vscode.service.MinuteService;

@RestController
@RequestMapping("/api/minutes")
public class MinuteRestController {

    @Autowired
    private MinuteService minuteService;

    @Autowired
    private AssociationService associationService;

    // GET all minutes
    @GetMapping("/")
    public ResponseEntity<List<MinuteDTO>> getAllMinutes() {
        List<MinuteDTO> minutes = minuteService.findAllDTOs();
        return ResponseEntity.ok(minutes);
    }

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
                                                  @RequestBody NewMinuteRequestDTO dto) {
        try {
            AssociationDTO associationDTO = associationService.findByIdDTO(id);

            MinuteDTO createdMinute = minuteService.createMinute(associationDTO, dto);

            return ResponseEntity.status(HttpStatus.CREATED).body(createdMinute);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null); // ou une réponse JSON avec message
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
