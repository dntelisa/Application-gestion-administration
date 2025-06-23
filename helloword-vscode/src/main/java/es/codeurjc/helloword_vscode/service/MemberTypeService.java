package es.codeurjc.helloword_vscode.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import es.codeurjc.helloword_vscode.dto.AssociationBasicDTO;
import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.dto.EditMTRequestDTO;
import es.codeurjc.helloword_vscode.dto.MemberDTO;
import es.codeurjc.helloword_vscode.dto.MemberMapper;
import es.codeurjc.helloword_vscode.dto.MemberTypeDTO;
import es.codeurjc.helloword_vscode.dto.MemberTypeLightDTO;
import es.codeurjc.helloword_vscode.dto.MemberTypeMapper;
import es.codeurjc.helloword_vscode.dto.NewMTRequestDTO;
import es.codeurjc.helloword_vscode.model.Association;
import es.codeurjc.helloword_vscode.model.Member;
import es.codeurjc.helloword_vscode.model.MemberType;
import es.codeurjc.helloword_vscode.repository.MemberTypeRepository;


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
  private AssociationService associationService;

  @Autowired
  private MemberTypeMapper memberTypeMapper;

  @Autowired
  private MemberMapper memberMapper;

  /* Save member type */
  public void save(MemberType memberType) {
    memberTypeRepository.save(memberType);
  }

  /* Create member type in web controller */
  public MemberTypeDTO createMemberType(MemberTypeDTO memberTypeDTO) {
      if(memberTypeDTO.id() != null) {
          throw new IllegalArgumentException();
      }
      MemberType memberType = toDomain(memberTypeDTO);
      memberType = memberTypeRepository.save(memberType);
      return toDTO(memberType);
  }

  /* Create member type in rest controller */
  public MemberTypeDTO createMemberType(NewMTRequestDTO mtRequestDTO, Authentication authentication) {

    // Retrieve member
    Member member = memberService.findById(mtRequestDTO.memberId())
            .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

    // Retrieve association
    Association association = associationService.findById(mtRequestDTO.associationId());

    // Check if the association already has any member types
    boolean associationIsEmpty = association.getMemberTypes().isEmpty();

    // Check if the member is already part of the association
    boolean alreadyInAssociation = association.getMemberTypes().stream()
            .anyMatch(mt -> mt.getMember().getId() == member.getId());

    if (alreadyInAssociation) {
        throw new IllegalArgumentException("Member is already part of this association.");
    }

    // Authorization: who is calling ?
    String loggedUsername = authentication.getName();

    boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

    if (isAdmin) {
        // Admin logic
        if (associationIsEmpty) {
            // If it's the first member, only president can be assigned
            if (!"president".equalsIgnoreCase(mtRequestDTO.memberType())) {
                throw new IllegalArgumentException("First member must be president.");
            }
        } else {
            // If assigning president, demote previous president
            if ("president".equalsIgnoreCase(mtRequestDTO.memberType())) {
                demotePreviousPresident(association, member);
            }
        }
    } else {
        // User logic

        // User can only add himself
        if (!member.getName().equalsIgnoreCase(loggedUsername)) {
            throw new SecurityException("You can only add yourself.");
        }

        String assignedRole;

        if (associationIsEmpty) {
            // If no members yet, user becomes president automatically
            assignedRole = "president";
        } else {
            assignedRole = "member";
        }

        // Force assigned role
        MemberType memberType = new MemberType(assignedRole, member, association);
        memberType = memberTypeRepository.save(memberType);
        return toDTO(memberType);
    }

    // Admin case: creation
    MemberType memberType = new MemberType(mtRequestDTO.memberType(), member, association);
    memberType = memberTypeRepository.save(memberType);
    return toDTO(memberType);
  }

  private void demotePreviousPresident(Association association, Member newPresident) {
    for (MemberType mt : association.getMemberTypes()) {
        if ("president".equalsIgnoreCase(mt.getName())
            && !(mt.getMember().getId() == (newPresident.getId()))) {
            mt.setName("member");
            memberTypeRepository.save(mt);
        }
    }
  }



  /* Delete member type */
  public void delete(MemberType memberType) {
    memberTypeRepository.delete(memberType);
  }

  public MemberTypeDTO deleteMemberType(long id, Authentication authentication) {
      // Retrieve the full MemberType entity
      MemberType memberType = memberTypeRepository.findById(id)
              .orElseThrow(() -> new ResourceNotFoundException("MemberType not found"));

      // Prevent deletion if the role is president
      if ("president".equalsIgnoreCase(memberType.getName())) {
          throw new SecurityException("You cannot delete the role of president.");
      }

      // Only the member himself or an admin can delete
      String loggedUsername = authentication.getName();

      boolean isAdmin = authentication.getAuthorities().stream()
              .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

      if (!memberType.getMember().getName().equalsIgnoreCase(loggedUsername) && !isAdmin) {
          throw new SecurityException("You are not allowed to delete this role.");
      }

      MemberTypeDTO memberTypeDTO = toDTO(memberType);

      memberTypeRepository.deleteById(id);

      return memberTypeDTO;
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

	/* Method to update member type for rest controller */
  public MemberTypeDTO updateMTDTO(Long mtId, EditMTRequestDTO dto, Authentication authentication) {

      // Retrieve the MemberType
      MemberType target = memberTypeRepository.findById(mtId)
              .orElseThrow(() -> new ResourceNotFoundException("Member type not found"));

      Long associationId = target.getAssociation().getId();

      // Only the association president or an admin can update roles
      String loggedUsername = authentication.getName();

      boolean isAdmin = authentication.getAuthorities().stream()
              .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

      boolean isPresident = target.getAssociation().getMemberTypes().stream()
              .anyMatch(mt -> "president".equalsIgnoreCase(mt.getName())
                      && mt.getMember().getName().equalsIgnoreCase(loggedUsername));

      if (!isAdmin && !isPresident) {
          throw new SecurityException("Only admin or association president can modify member types.");
      }

      // Prevent self-demotion for presidents
      if ("president".equalsIgnoreCase(target.getName())
          && !target.getName().equalsIgnoreCase(dto.name())) {
          throw new SecurityException("If you want to change role, promote someone else first.");
      }

      // Handle new president promotion and demotion of previous one
      if ("president".equalsIgnoreCase(dto.name())) {
          Collection<MemberTypeDTO> memberTypes = findByAssociationIdDTO(associationId);
          for (MemberTypeDTO mt : memberTypes) {
              if ("president".equalsIgnoreCase(mt.name())
                      && !mt.member().id().equals(target.getMember().getId())) {
                  MemberType previousPresident = findById(mt.id()).orElseThrow();
                  previousPresident.setName("member");
                  save(previousPresident);
              }
          }
      }

      target.setName(dto.name());
      save(target);

      return toDTO(target);
  }


  /* Find all member types */
  public Collection<MemberTypeDTO> findAllMTDTOs() {
    return toDTOs(memberTypeRepository.findAll());
  }

    /* Find all member types with pagination */
    public Page<MemberTypeDTO> getAllMemberTypes(Pageable pageable) {
        return memberTypeRepository.findAll(pageable)
                .map(memberTypeMapper::toDTO);
    }


  /* Find one member type by id */
  public MemberTypeDTO findByIdMTDTO(long id) {
    return toDTO(memberTypeRepository.findById(id).orElseThrow());
  }

  public void addUserToAssociation(Long associationId, String username) {

        AssociationBasicDTO associationDTO = associationService.findByIdDTOBasic(associationId);
        MemberDTO memberDTO = memberService.findByNameDTO(username);

        List<MemberDTO> currentMembers = memberService.findMembersByAssociationId(associationId);

        boolean alreadyMember = currentMembers.stream()
                .anyMatch(member -> member.id().equals(memberDTO.id()));

        if (!alreadyMember) {
            String role = currentMembers.isEmpty() ? "president" : "member";
            MemberTypeDTO newMemberType = new MemberTypeDTO(null, role, memberDTO, associationDTO);
            createMemberType(newMemberType);
        }
    }

    public void deleteUserFromAssociation(Long associationId, Long userId) {

        MemberDTO memberDTO = memberService.findByIdDTO(userId);

        Collection<MemberTypeDTO> memberTypes = findByMemberDTO(memberDTO);

        List<MemberTypeDTO> typesToDelete = memberTypes.stream()
                .filter(mt -> mt.association().id().equals(associationId))
                .toList();

        for (MemberTypeDTO mt : typesToDelete) {
            if ("president".equalsIgnoreCase(mt.name())) {
                throw new IllegalStateException("You must choose a new president before leaving the association.");
            }
            delete(memberTypeMapper.toDomain(mt));
        }
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

