package es.codeurjc.helloword_vscode.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.hibernate.engine.jdbc.BlobProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import es.codeurjc.helloword_vscode.dto.AssociationBasicDTO;
import es.codeurjc.helloword_vscode.dto.AssociationBasicMapper;
import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.dto.AssociationMapper;
import es.codeurjc.helloword_vscode.dto.PagedResponseDTO;
import es.codeurjc.helloword_vscode.model.Association;
import es.codeurjc.helloword_vscode.model.Member;
import es.codeurjc.helloword_vscode.repository.AssociationRepository;
import es.codeurjc.helloword_vscode.repository.MemberRepository;
import es.codeurjc.helloword_vscode.repository.MinuteRepository;

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
		if (association.getMemberTypes() != null) {
			association.getMemberTypes().forEach(mt -> {
				Member fullMember = memberRepository.findById(mt.getMember().getId()).orElseThrow();
				mt.setMember(fullMember);
			});
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
	public Association findById(long id) {
		return associationRepository.findById(id)
		.orElseThrow(() -> new ResourceNotFoundException("Association not found with id: " + id));
	}

	/* Find association by ID */
	public AssociationDTO findByIdDTO(long id) {
    	return toDTO(associationRepository.findById(id).orElseThrow());
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

	/* Load details of an association */
	public AssociationDTO getDetailedAssociationDTO(long id) {
    return toDTO(associationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Association with id " + id + " not found")));
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

	/* Add an image in association */
	public void createAssoImage(long id, URI location, InputStream inputStream, long size) {

		Association association = associationRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Association not found"));

		association.setImage(true);
		association.setImagePath(location.toString());
		association.setImageFile(BlobProxy.generateProxy(inputStream, size));

		associationRepository.save(association);
	}

	/* Retrieve an image of association */
	public Resource getAssociationImage(long id) throws SQLException {

		Association association = associationRepository.findById(id).orElseThrow();

		if (association.getImageFile() != null) {
			return new InputStreamResource(association.getImageFile().getBinaryStream());
		} else {
			throw new NoSuchElementException();
		}
	}

	/* Edit image of an association in REST */
	public void replaceAssociationImage(long id, InputStream inputStream, long size) {
		Association association = associationRepository.findById(id).orElseThrow();
		if(!association.getImage()){
			throw new NoSuchElementException();
		}
		association.setImage(true);
		association.setImageFile(BlobProxy.generateProxy(inputStream, size));
		associationRepository.save(association);
	}

	/* Delete image of an association rest */
	public void deleteAssociationImage(long id) {

		Association association = associationRepository.findById(id).orElseThrow();

		if(!association.getImage()){
			throw new NoSuchElementException();
		}

		association.setImageFile(null);
		association.setImagePath(null);
		association.setImage(false);

		associationRepository.save(association);
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

	
	public PagedResponseDTO<AssociationBasicDTO> getPagedAssociations(Pageable pageable) {
		Page<Association> page = associationRepository.findAll(pageable);

		List<AssociationBasicDTO> dtoList = page.getContent().stream()
			.map(associationBasicMapper::toDTO)
			.collect(Collectors.toList());

		return new PagedResponseDTO<>(
			dtoList,
			page.getNumber(),
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages(),
			page.isLast(),
			page.isFirst()
		);
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
