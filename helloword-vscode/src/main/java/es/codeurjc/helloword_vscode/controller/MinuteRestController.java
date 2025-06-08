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
import java.util.List;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import es.codeurjc.helloword_vscode.dto.MinuteDTO;
import es.codeurjc.helloword_vscode.service.MinuteService;

@RestController
@RequestMapping("/api/minutes")
public class MinuteRestController {

    @Autowired
    private MinuteService minuteService;

    @GetMapping("/")
    public ResponseEntity<List<MinuteDTO>> getAllMinutes() {
        List<MinuteDTO> minutes = minuteService.findAllDTOs();
        return ResponseEntity.ok(minutes);
    }
}
