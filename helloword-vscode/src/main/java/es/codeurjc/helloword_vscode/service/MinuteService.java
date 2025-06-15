package es.codeurjc.helloword_vscode.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import es.codeurjc.helloword_vscode.dto.MinuteDTO;
import es.codeurjc.helloword_vscode.dto.MemberDTO;
import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.dto.MinuteMapper;
import es.codeurjc.helloword_vscode.dto.NewMinuteRequestDTO;
import es.codeurjc.helloword_vscode.dto.MemberMapper;
import es.codeurjc.helloword_vscode.dto.AssociationMapper;
import es.codeurjc.helloword_vscode.dto.EditMinuteRequestDTO;
import es.codeurjc.helloword_vscode.model.Association;
import es.codeurjc.helloword_vscode.model.Member;
import es.codeurjc.helloword_vscode.model.Minute;
import es.codeurjc.helloword_vscode.repository.MinuteRepository;

/**
 * This service class provides methods to perform various operations on Minute entities,
 * such as saving, retrieving, and deleting minutes. It interacts with the MinuteRepository
 * and AssociationService to perform database operations.
*/
@Service
public class MinuteService {
	// Autowired repositories and services for database interactions
    @Autowired
	private MinuteRepository minuteRepository;

	@Autowired
	private AssociationService associationService;

	@Autowired
	private AssociationMapper associationMapper;

	@Autowired
	private MemberService memberService;

	@Autowired
	private MinuteMapper minuteMapper;

	@Autowired
	private MemberMapper memberMapper;

	/* Find all minutes */
	public List<MinuteDTO> findAllDTOs() {
		List<Minute> minutes = minuteRepository.findAll();
		return minuteMapper.toDTOs(minutes);
	}


	/* Save minute */
	public void save (Minute minute) throws IOException{
		minuteRepository.save(minute);
	}

	/* Find minute by ID */
	public MinuteDTO findByIdDTO(long id){
		return toDTO(minuteRepository.findById(id).orElseThrow());
	}

	/* Delete minute with association and minute ID */
	public MinuteDTO deleteMinuteByIdDTO(Long minuteId, Long assoId) {
        Minute minute = minuteRepository.findById(minuteId)
            .orElseThrow(() -> new ResourceNotFoundException("Minute not found with id: " + minuteId));
        
		MinuteDTO minuteDTO = toDTO(minute);

		// Retrieve the association by ID
		Association association = associationService.findById(assoId)
		.orElseThrow(() -> new ResourceNotFoundException("Association not found"));

        // Remove the minute from the association's list of minutes
		association.getMinutes().remove(minute);
		
        for (Member member : minute.getParticipants()) {
            member.getMinutes().remove(minute);
        }
        minuteRepository.delete(minute);

		return minuteDTO;
    }

	/* Delete minute with the controller rest */
	public MinuteDTO deleteMinuteByIdDTORest(Long minuteId) {
		Minute minute = minuteRepository.findById(minuteId)
			.orElseThrow(() -> new ResourceNotFoundException("Minute not found with id: " + minuteId));

		MinuteDTO minuteDTO = toDTO(minute);

		// Dissociate the minute from the association
		Association association = minute.getAssociation();
		if (association != null) {
			association.getMinutes().remove(minute);
		}

		// Dissociate the participants
		for (Member member : minute.getParticipants()) {
			member.getMinutes().remove(minute);
		}

		// Delete the minute
		minuteRepository.delete(minute);

		return minuteDTO;
	}



	/* Find all Minute entities that contain the specified participant */
	List<Minute> findAllByParticipantsContains(Member participant){
		return minuteRepository.findAllByParticipantsContains(participant);
	}

	/* Methode to create a new minute */
	public MinuteDTO createMinute(AssociationDTO associationDTO, NewMinuteRequestDTO dto) {

		LocalDate date;
		try {
			date = LocalDate.parse(dto.date());
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("Invalid date format.");
		}

		if (date.isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("The date cannot be in the future.");
		}

		Association association = toDomain(associationDTO);

		List<Member> participants = dto.participantsIds().stream()
			.map(id -> memberService.findById(id).orElse(null))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());

		Minute minute = new Minute();
		minute.setDate(dto.date());
		minute.setParticipants(participants);
		minute.setContent(dto.content());
		minute.setDuration(dto.duration());
		minute.setAssociation(association);

		minuteRepository.save(minute);

		return toDTO(minute);
	}

	/* Method to find all the members of an association */
	public List<MemberDTO> findMembersDTO(AssociationDTO associationDTO){
			return memberService.findMembersByAssociationId(associationDTO.id());
		}


		/* Find participants of a meeting */
		public List<MemberDTO> findParticipantsDTO(MinuteDTO minuteDTO){
			return minuteDTO.participants();
		}

		public List<MemberDTO> findNoParticipantsDTO(AssociationDTO associationDTO, MinuteDTO minuteDTO){
			List<MemberDTO> members = memberService.findMembersByAssociationId(associationDTO.id());
			Set<Long> participantIds = minuteDTO.participants().stream().map(MemberDTO::id).collect(Collectors.toSet());
			return members.stream().filter(m -> !participantIds.contains(m.id())).collect(Collectors.toList());
	}

	/* Method to update minute for web controller */
	public void updateDTO(EditMinuteRequestDTO dto) throws IOException {
		// Retrieving the minute
		Minute minute = minuteRepository.findById(dto.minuteId())
			.orElseThrow(() -> new ResourceNotFoundException("Minute not found"));

		// Retrieving association with DTO
		AssociationDTO associationDTO = associationService.findByIdDTO(dto.assoId());
		Association association = toDomain(associationDTO);

		// Verify and convert date
		LocalDate date;
		try {
			date = LocalDate.parse(dto.date());
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("Invalid date format.");
		}

		if (date.isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("The date cannot be in the future.");
		}

		// Retrieving of the members
		List<Member> participants = dto.participantsIds().stream()
			.map(id -> memberService.findById(id).orElse(null))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());

		// Update minute
		minute.setDate(dto.date());
		minute.setParticipants(participants);
		minute.setContent(dto.content());
		minute.setDuration(dto.duration());
		minute.setAssociation(association);

		minuteRepository.save(minute);
	}


	/* Method to update minute for rest controller */
	public MinuteDTO updateDTO(Long minuteId, EditMinuteRequestDTO dto) {
		Minute minute = minuteRepository.findById(minuteId)
			.orElseThrow(() -> new ResourceNotFoundException("Minute not found"));

		// Parse date
		LocalDate date;
		try {
			date = LocalDate.parse(dto.date());
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("Invalid date format.");
		}

		if (date.isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("The date cannot be in the future.");
		}

		// Fetch participants
		List<Member> participants = dto.participantsIds().stream()
			.map(id -> memberService.findById(id).orElse(null))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());

		// Update fields
		minute.setDate(dto.date());
		minute.setParticipants(participants);
		minute.setContent(dto.content());
		minute.setDuration(dto.duration());

		// Save and return DTO
		return toDTO(minuteRepository.save(minute));
	}

	/* Convert entity to DTO */
	public MinuteDTO toDTO(Minute minute) {
		return minuteMapper.toDTO(minute);
	}

	/* Convert a minutes set to DTOs */
	private Collection<MinuteDTO> toDTOs(Collection<Minute> minutes) {
		return minuteMapper.toDTOs(minutes);
	}

	/* Convert a DTO to entity */
	public Minute toDomain(MinuteDTO minuteDTO){
		return minuteMapper.toDomain(minuteDTO);
	}

	/* Convert a DTO to entity */
	public Member toDomainMember(MemberDTO memberDTO){
		return memberMapper.toDomain(memberDTO);
	}

	/* Converted a DTO to entity */
	private Association toDomain(AssociationDTO associationDTO){
		return associationMapper.toDomain(associationDTO);
	}

}