package es.codeurjc.helloword_vscode.service;

import org.hibernate.engine.jdbc.BlobProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import es.codeurjc.helloword_vscode.repository.AssociationRepository;
import es.codeurjc.helloword_vscode.repository.MemberRepository;
import es.codeurjc.helloword_vscode.repository.MinuteRepository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import es.codeurjc.helloword_vscode.dto.AssociationBasicDTO;
import es.codeurjc.helloword_vscode.dto.AssociationBasicMapper;
import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.dto.AssociationMapper;
import es.codeurjc.helloword_vscode.dto.MemberDTO;
import es.codeurjc.helloword_vscode.dto.MemberTypeDTO;
import es.codeurjc.helloword_vscode.model.Association;
import es.codeurjc.helloword_vscode.model.MemberType;
import es.codeurjc.helloword_vscode.model.Member;

/* 
 * This service class provides methods to perform various operations on
 * the Association entity, such as saving, retrieving, and deleting 
 * associations. It interacts with the AssociationRepository to perform 
 * database operations.
*/
@Service
public class AssociationService {

	// Autowired repository for database interactions
    @Autowired
	private AssociationRepository associationRepository;

	@Autowired
	private MinuteRepository minuteRepository;

	@Autowired
	private MemberRepository memberRepository;	

    @Autowired
	@Lazy
    private MemberService memberService;

    @Autowired
    private MemberTypeService memberTypeService;

	@Autowired
	private AssociationMapper associationMapper;

	@Autowired
	private AssociationBasicMapper associationBasicMapper;

	/* Save association without image */
	public AssociationDTO createAsso(AssociationDTO associationDTO) {
		if(associationDTO.id() != null) {
			throw new IllegalArgumentException();
		}
		Association association = toDomain(associationDTO); // convert to domain
		associationRepository.save(association);
		if (association.getMinutes() != null) {
			association.getMinutes().replaceAll(minute -> minuteRepository.findById(minute.getId()).orElseThrow());
		}
		if (association.getMembers() != null) {
			association.getMembers().replaceAll(member -> memberRepository.findById(member.getId()).orElseThrow());
		}
		return toDTO(association); // convert to DTO
	}

	/* Save association with image */
	public void createAssociationImage(long id, InputStream inputStream, long size) {

		Association association = associationRepository.findById(id).orElseThrow();

		association.setImage(true);
		association.setImageFile(BlobProxy.generateProxy(inputStream, size));

		associationRepository.save(association);
	}

	public AssociationDTO createOrReplaceAssociation(Long id, AssociationDTO associationDTO) throws SQLException {
		
		AssociationDTO association;
		if(id == null) {
			association = createAsso(associationDTO);
		} else {
			association = replaceAssociation(id, associationDTO);
		}
		return association;
	}


	/* Find association by ID */
	public Optional<Association> findById(long id) {
		return associationRepository.findById(id);
	}

	/* Find association by ID */
	public AssociationDTO findByIdDTO(long id) {
    	Association asso = associationRepository.findById(id)
        	.orElseThrow(() -> new ResourceNotFoundException("Association not found"));

    	return associationMapper.toDTO(asso);
	}

	/* Find association by ID */
	public AssociationBasicDTO findByIdDTOBasic(long id) {
		return toDTOAssociationBasic(associationRepository.findById(id).orElseThrow());
	}


	/* Find all associations */
	public Collection<AssociationDTO> findAllDTOs() {
    	return toDTOs(associationRepository.findAll());
	}


	/* Delete association by ID */
	public AssociationDTO deleteAssociation(long id) {

		Association association = associationRepository.findById(id).orElseThrow();

		//As associations are related to minutes and member types, 
		// it is needed to load the association minutes and member types 
		//before deleting it to avoid LazyInitializationException
		AssociationDTO associationDTO = toDTO(association);

		associationRepository.deleteById(id);

		return associationDTO;
	}

	/* Add user to an association */
	public void addUserToAssociation(Long associationId, String userId) {
		AssociationBasicDTO associationDTO = findByIdDTOBasic(associationId);
		MemberDTO memberDTO = memberService.findByNameDTO(userId);
		List<MemberDTO> currentMembers = memberService.findMembersByAssociationId(associationId);
		// Verify if the user isn't already in the association
		boolean alreadyMember = currentMembers.stream()
        .anyMatch(member -> member.id().equals(memberDTO.id()));

		// If the user is the first one to join an association he will become president else he will be member
		if (!alreadyMember) {
        	String role = currentMembers.isEmpty() ? "president" : "member";
        	MemberTypeDTO newMemberType = new MemberTypeDTO(null, role, memberDTO, associationDTO);
        	memberTypeService.createMemberType(newMemberType);
    	}
	}

	@Transactional
	public void deleteUserFromAssociationDTO(Long associationId, Long userId) {

		Member member = memberService.findById(userId)
			.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		// On accède aux MemberType via l'entité, sinon le DTO n'a pas la relation
		List<MemberType> memberTypes = member.getMemberTypes().stream()
			.filter(mt -> Objects.equals(mt.getAssociation().getId(), associationId))
			.toList();

		for (MemberType mt : memberTypes) {
			if ("president".equalsIgnoreCase(mt.getName())) {
				throw new IllegalStateException("You must choose a new president before leaving the association");
			}
			memberTypeService.delete(mt); // méthode qui accepte une entité
		}
	}

	/* Load details of an association */
	public AssociationDTO getDetailedAssociationDTO(Long associationId) {
		return findByIdDTO(associationId);
	}

	/* Update the data of an association */
	public AssociationDTO replaceAssociation(long id, AssociationDTO updatedAssociationDTO) throws SQLException {

		Association oldAssociation = associationRepository.findById(id).orElseThrow();
		Association updatedAssociation = toDomain(updatedAssociationDTO);
		updatedAssociation.setId(id);

		updatedAssociation.setMemberTypes(oldAssociation.getMemberTypes());
		updatedAssociation.setMinutes(oldAssociation.getMinutes());

		if (oldAssociation.getImage() && oldAssociation.getImageFile() != null) {
			updatedAssociation.setImage(true);
			updatedAssociation.setImageFile(
				BlobProxy.generateProxy(
					oldAssociation.getImageFile().getBinaryStream(),
					oldAssociation.getImageFile().length()
				)
			);
		}

		associationRepository.save(updatedAssociation);
		return toDTO(updatedAssociation);
	}



	/* Edit an association with image */
	public void updateAssoImage(Association association, String name, MultipartFile multipartFile) throws IOException {
		association.setName(name);
		if(!multipartFile.isEmpty()) {
			// Set the image file as a Blob in the association
			association.setImageFile(BlobProxy.generateProxy(multipartFile.getInputStream(), multipartFile.getSize()));
			association.setImage(true);
		}
		associationRepository.save(association);
	}

	/* Recover the image of an association */
	public Resource getImage(Long id) throws SQLException {
		Association association = associationRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Association not found"));

		Blob imageBlob = association.getImageFile();

		if (imageBlob != null) {
			return new InputStreamResource(imageBlob.getBinaryStream());
		} else {
			throw new NoSuchElementException("Image file is null");
		}
	}

	/* Delete image from association */
	public void deleteImage(Long id) {
		Association association = associationRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Association not found"));

		association.setImageFile(null);
		association.setImage(false);
		associationRepository.save(association);
	}

	/* Convert entity to DTO */
	private AssociationDTO toDTO(Association association) {
		return associationMapper.toDTO(association);
	}

	private AssociationBasicDTO toDTOAssociationBasic(Association association) {
		return associationBasicMapper.toDTO(association);
	}

	/* Converted an association set to DTOs */
	private Collection<AssociationDTO> toDTOs(Collection<Association> associations) {
		return associationMapper.toDTOs(associations);
	}

	/* Converted a DTO to entity */
	private Association toDomain(AssociationDTO associationDTO){
		return associationMapper.toDomain(associationDTO);
	}
}
