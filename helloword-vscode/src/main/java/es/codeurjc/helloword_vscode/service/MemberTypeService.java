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
import es.codeurjc.helloword_vscode.dto.AssociationMapper;
import es.codeurjc.helloword_vscode.dto.EditMTRequestDTO;
import es.codeurjc.helloword_vscode.dto.MemberDTO;
import es.codeurjc.helloword_vscode.dto.MemberMapper;
import es.codeurjc.helloword_vscode.dto.MemberTypeDTO;
import es.codeurjc.helloword_vscode.dto.MemberTypeLightDTO;
import es.codeurjc.helloword_vscode.dto.MemberTypeMapper;
import es.codeurjc.helloword_vscode.dto.NewMTRequestDTO;
import es.codeurjc.helloword_vscode.dto.AssociationBasicMapper;
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
    private AssociationService associationService;

    @Autowired
    private MemberTypeMapper memberTypeMapper;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private AssociationBasicMapper associationBasicMapper;

    @Autowired
    private AssociationMapper associationMapper;


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
    public MemberTypeDTO createMemberType(MemberDTO memberDTO, AssociationDTO associationDTO, String requestedRole, Authentication authentication) {

    boolean associationIsEmpty = associationDTO.memberTypes().isEmpty();

    boolean alreadyInAssociation = associationDTO.memberTypes().stream()
            .anyMatch(mt -> mt.member().id().equals(memberDTO.id()));

    if (alreadyInAssociation) {
        throw new IllegalArgumentException("Member is already part of this association.");
    }

    String loggedUsername = authentication.getName();

    boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

    String roleToAssign = requestedRole;

    if (isAdmin) {
        if (associationIsEmpty && !"president".equalsIgnoreCase(requestedRole)) {
            throw new IllegalArgumentException("First member must be president.");
        }

        if ("president".equalsIgnoreCase(requestedRole)) {
            demotePreviousPresident(associationDTO, memberDTO);
        }

    } else {
        if (!memberDTO.name().equalsIgnoreCase(loggedUsername)) {
            throw new SecurityException("You can only add yourself.");
        }

        roleToAssign = associationIsEmpty ? "president" : "member";
    }

    MemberTypeDTO newMT = new MemberTypeDTO(
        null,
        roleToAssign,
        memberDTO,
        new AssociationBasicDTO(associationDTO.id(), associationDTO.name())
    );

        return createMemberType(newMT);
    }


    private void demotePreviousPresident(AssociationDTO associationDTO, MemberDTO newPresident) {
        for (MemberTypeLightDTO mt : associationDTO.memberTypes()) {
            if ("president".equalsIgnoreCase(mt.name())
                && !mt.member().id().equals(newPresident.id())) {

                MemberType existing = memberTypeRepository.findById(mt.id())
                        .orElseThrow(() -> new ResourceNotFoundException("MemberType not found"));

                existing.setName("member");
                memberTypeRepository.save(existing);
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
    public void changeMemberRole(AssociationDTO associationDTO, MemberDTO requesterDTO, Long memberTypeId, String newRole) {

        MemberTypeDTO targetMemberTypeDTO = findByIdDTO(memberTypeId);

        if (!targetMemberTypeDTO.association().id().equals(associationDTO.id())) {
            throw new IllegalArgumentException("Mismatched association and memberType");
        }

        boolean requesterIsPresident = findByMemberDTO(requesterDTO).stream()
            .anyMatch(mt -> mt.association().id().equals(associationDTO.id())
                        && "president".equalsIgnoreCase(mt.name()));

        if (!requesterIsPresident) {
            throw new SecurityException("Only the president can change roles.");
        }

        if (requesterDTO.id().equals(targetMemberTypeDTO.member().id())
            && "president".equalsIgnoreCase(targetMemberTypeDTO.name())) {
            throw new SecurityException("If you want to change your role, promote someone else first.");
        }

        if ("president".equalsIgnoreCase(newRole)) {
            Collection<MemberTypeDTO> memberTypes = findByAssociationIdDTO(associationDTO.id());
            for (MemberTypeDTO mt : memberTypes) {
                if ("president".equalsIgnoreCase(mt.name())
                    && !mt.member().id().equals(targetMemberTypeDTO.member().id())) {

                    MemberType updated = findById(mt.id()).orElseThrow();
                    updated.setName("member");
                    save(updated);
                }
            }
        }

        MemberType target = findById(memberTypeId).orElseThrow();
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

    /* Find all member types with pagination */
    public Page<MemberTypeDTO> getAllMemberTypes(Pageable pageable) {
        return memberTypeRepository.findAll(pageable)
                .map(memberTypeMapper::toDTO);
    }


    /* Find one member type by id */
    public MemberTypeDTO findByIdMTDTO(long id) {
    return toDTO(memberTypeRepository.findById(id).orElseThrow());
    }

    public void addUserToAssociation(AssociationDTO associationDTO, MemberDTO memberDTO) {

    List<MemberDTO> currentMembers = associationDTO.memberTypes().stream()
            .map(MemberTypeLightDTO::member)
            .toList();

    boolean alreadyMember = currentMembers.stream()
            .anyMatch(member -> member.id().equals(memberDTO.id()));

    if (!alreadyMember) {
        String role = currentMembers.isEmpty() ? "president" : "member";

        // Convert AssociationDTO -> AssociationBasicDTO
        AssociationBasicDTO associationBasicDTO = associationBasicMapper.toDTO(associationMapper.toDomain(associationDTO));

        MemberTypeDTO newMemberType = new MemberTypeDTO(
            null,
            role,
            memberDTO,
            associationBasicDTO
        );

        createMemberType(newMemberType);
    }
    }



    public void deleteUserFromAssociation(Long associationId, Long userId) {

        List<MemberType> typesToDelete = memberTypeRepository.findByMemberIdAndAssociationId(userId, associationId);

        for (MemberType mt : typesToDelete) {
            if ("president".equalsIgnoreCase(mt.getName())) {
                throw new IllegalStateException("You must choose a new president before leaving the association.");
            }

            memberTypeRepository.delete(mt);
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

