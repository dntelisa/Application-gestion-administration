package es.codeurjc.helloword_vscode.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.codeurjc.helloword_vscode.repository.MemberTypeRepository;

import es.codeurjc.helloword_vscode.model.MemberType;
import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.dto.MemberDTO;
import es.codeurjc.helloword_vscode.dto.MemberMapper;
import es.codeurjc.helloword_vscode.dto.MemberTypeDTO;
import es.codeurjc.helloword_vscode.dto.MemberTypeLightDTO;
import es.codeurjc.helloword_vscode.dto.MemberTypeMapper;
import es.codeurjc.helloword_vscode.model.Association;
import es.codeurjc.helloword_vscode.model.Member;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/* 
 * This service class provides methods to perform various operations on
 * the MemberType entity, such as saving member types. It interacts with 
 * the MemberTypeRepository to perform database operations.
*/
@Service
public class MemberTypeService {
  // Autowired repository for database interactions
  @Autowired
  private MemberTypeRepository memberTypeRepository;

  @Autowired
  @Lazy
  private MemberService memberService;

  @Autowired
  @Lazy
  private AssociationService associationService;

  @Autowired
  private MemberTypeMapper memberTypeMapper;

  @Autowired
  private MemberMapper memberMapper;

  /* Save member type */
  public void save(MemberType memberType) {
    memberTypeRepository.save(memberType);
  }

  public MemberTypeDTO createMemberType(MemberTypeDTO memberTypeDTO) {

      if(memberTypeDTO.id() != null) {
          throw new IllegalArgumentException();
      }

      MemberType memberType = toDomain(memberTypeDTO);

      memberType = memberTypeRepository.save(memberType);

      return toDTO(memberType);
  }


  /* Delete member type */
  public void delete(MemberType memberType) {
    memberTypeRepository.delete(memberType);
  }

      public MemberTypeDTO deleteMemberType(long id) {

        MemberType memberType = memberTypeRepository.findById(id).orElseThrow();

        memberTypeRepository.deleteById(id);

        return toDTO(memberType);
    }


  /* Find all member type from user */
  public List<MemberType> findByMember(Member member) {
    return memberTypeRepository.findByMember(member);
  }

  public Collection<MemberTypeDTO> findByMemberDTO(MemberDTO memberDTO) {
      Member member = memberMapper.toDomain(memberDTO);
      return toDTOs(memberTypeRepository.findByMember(member));
  }

  /* Find the president of an association */
  public Optional<Member> getPresident(Association association) {
    return association.getMemberTypes().stream()
        .filter(mt -> "president".equalsIgnoreCase(mt.getName()))
        .map(MemberType::getMember)
        .findFirst();
  }

  public MemberDTO getPresidentDTO(AssociationDTO associationDTO) {
    return associationDTO.memberTypes().stream()
        .filter(mt -> "president".equalsIgnoreCase(mt.name()))
        .map(MemberTypeLightDTO::member)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No president found for the association"));
  }

  /* Find MemberType by id*/
  public Optional<MemberType> findById(long id) {
		return memberTypeRepository.findById(id);
	}

  public MemberTypeDTO findByIdDTO(long id) {
		return toDTO(memberTypeRepository.findById(id).orElseThrow());
	}

  public Collection<MemberTypeDTO> findByAssociationIdDTO(Long associationId) {
    return associationService.findById(associationId)
        .orElseThrow()
        .getMemberTypes()
        .stream()
        .map(memberTypeMapper::toDTO)
        .collect(Collectors.toList());
  }

  @Transactional
  public void changeMemberRole(Long associationId, String requesterUsername, Long memberTypeId, String newRole) {
      MemberDTO requesterDTO = memberService.findByNameDTO(requesterUsername);
      MemberTypeDTO targetMemberTypeDTO = findByIdDTO(memberTypeId);

      if (!targetMemberTypeDTO.association().id().equals(associationId)) {
          throw new IllegalArgumentException("Mismatched association and memberType");
      }
      // Verify if the asker is president
      boolean requesterIsPresident = findByMemberDTO(requesterDTO).stream()
          .anyMatch(mt -> mt.association().id().equals(associationId)
                      && "president".equalsIgnoreCase(mt.name()));
      if (!requesterIsPresident) {
          throw new SecurityException("Only the president can change roles.");
      }
      // President can't change his own role
      if (requesterDTO.id().equals(targetMemberTypeDTO.member().id())
          && "president".equalsIgnoreCase(targetMemberTypeDTO.name())) {
          throw new SecurityException("If you want to change your role, promote someone else first.");
      }
      // If new president, older become member
      if ("president".equalsIgnoreCase(newRole)) {
          Collection<MemberTypeDTO> memberTypes = findByAssociationIdDTO(associationId);
          for (MemberTypeDTO mt : memberTypes) {
              if ("president".equalsIgnoreCase(mt.name())
                  && !mt.member().id().equals(targetMemberTypeDTO.member().id())) {
                  MemberType updated = findById(mt.id()).orElseThrow(); // entity temporary for
                  updated.setName("member");
                  save(updated);
              }
          }
      }
      // Update role
      MemberType target = findById(memberTypeId).orElseThrow(); // entity to update
      target.setName(newRole);
      save(target);
  }

  
	/* Convert entity to DTO */
	private MemberTypeDTO toDTO(MemberType memberType) {
		return memberTypeMapper.toDTO(memberType);
	}

  /* Convert entity to DTO */
	private MemberDTO toDTO(Member member) {
		return memberMapper.toDTO(member);
	}

	/* Converted an association set to DTOs */
	private Collection<MemberTypeDTO> toDTOs(Collection<MemberType> memberType) {
		return memberTypeMapper.toDTOs(memberType);
	}

	/* Converted a DTO to entity */
	private MemberType toDomain(MemberTypeDTO memberTypeDTO){
		return memberTypeMapper.toDomain(memberTypeDTO);
	}
}

