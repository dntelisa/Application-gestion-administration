package es.codeurjc.helloword_vscode.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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


	/* Find all Minute entities that contain the specified participant */
	List<Minute> findAllByParticipantsContains(Member participant){
		return minuteRepository.findAllByParticipantsContains(participant);
	}

	/* Create new minute */
	public Map<String, Object> processCreateMinute(Association association, String dateStr, List<Long> participantIds, String content, double duration) {
		Map<String, Object> model = new HashMap<>();
		model.put("association", association);
		model.put("members", association.getMembers());

		try {
			LocalDate date = LocalDate.parse(dateStr);
			if (date.isAfter(LocalDate.now())) {
				model.put("error", "The date can not be in the futur");
				return model;
			}

			Minute minute = new Minute();
			minute.setDate(dateStr);
			List<Member> participants = participantIds.stream()
				.map(id -> memberService.findById(id).orElse(null))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
			minute.setParticipants(participants);
			minute.setContent(content);
			minute.setDuration(duration);
			minute.setAssociation(association);

			minuteRepository.save(minute);
			return model;
		} catch (DateTimeParseException e) {
			model.put("error", "Invalid date format.");
			return model;
		}
	}
	

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

	public void updateDTO(MinuteDTO minuteDTO, String date, List<Long> participantsIds, String content, double duration, Association association){ //ne marche pas avec AssociationDTO -> pas d'explications
        // Update minute attributes
        Minute minute = toDomain(minuteDTO);
		//Association association = toDomain(associationDTO);
		minute.setDate(date);
        List<Member> participants = participantsIds.stream()
            .map(participantId -> memberService.findById(participantId).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        minute.setParticipants(participants);
        minute.setContent(content);
        minute.setDuration(duration);
        minute.setAssociation(association);
		minuteRepository.save(minute);
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