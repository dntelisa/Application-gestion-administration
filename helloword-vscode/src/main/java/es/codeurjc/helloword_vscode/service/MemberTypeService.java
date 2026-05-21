package es.codeurjc.helloword_vscode.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import es.codeurjc.helloword_vscode.dto.AssociationBasicDTO;
import es.codeurjc.helloword_vscode.dto.AssociationBasicMapper;
import es.codeurjc.helloword_vscode.dto.AssociationDTO;
import es.codeurjc.helloword_vscode.dto.AssociationMapper;
import es.codeurjc.helloword_vscode.dto.EditMTRequestDTO;
import es.codeurjc.helloword_vscode.dto.MemberDTO;
import es.codeurjc.helloword_vscode.dto.MemberMapper;
import es.codeurjc.helloword_vscode.dto.MemberTypeDTO;
import es.codeurjc.helloword_vscode.dto.MemberTypeLightDTO;
import es.codeurjc.helloword_vscode.dto.MemberTypeMapper;
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

        // Check if the association currently has no members
        boolean associationIsEmpty = associationDTO.memberTypes().isEmpty();

        boolean alreadyInAssociation = associationDTO.memberTypes().stream()
                .anyMatch(mt -> mt.member().id().equals(memberDTO.id()));

        // Check if the member is already part of the association
        if (alreadyInAssociation) {
            throw new IllegalArgumentException("Member is already part of this association.");
        }

        // Retrieve the logged-in user's username
        String loggedUsername = authentication.getName();

        // Check if the logged-in user is an admin
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        // Determine the role to assign
        String roleToAssign = requestedRole;

        if (isAdmin) {
            // If the association is empty and the requested role is not president, throw an exception
            if (associationIsEmpty && !"president".equalsIgnoreCase(requestedRole)) {
                throw new IllegalArgumentException("First member must be president.");
            }

            // If the requested role is president, demote the previous president
            if ("president".equalsIgnoreCase(requestedRole)) {
                demotePreviousPresident(associationDTO, memberDTO);
            }

        } else {

            // If the user is not an admin, they can only add themselves
            if (!memberDTO.name().equalsIgnoreCase(loggedUsername)) {
                throw new SecurityException("You can only add yourself.");
            }

            // If the association is empty, assign the role of president; otherwise, assign the role of member
            roleToAssign = associationIsEmpty ? "president" : "member";
        }

        // Create a new MemberTypeDTO with the determined role
        MemberTypeDTO newMT = new MemberTypeDTO(
            null,
            roleToAssign,
            memberDTO,
            new AssociationBasicDTO(associationDTO.id(), associationDTO.name())
        );

        // Save and return the new member type
        return createMemberType(newMT);
    }

    /* If a new president is promoved, old one is demoted */
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

    /* Delete member type in rest controller */
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

    /* Find all member type from user DTO and return a collection of member DTO*/
    public Collection<MemberTypeDTO> findByMemberDTO(MemberDTO memberDTO) {
        Member member = memberMapper.toDomain(memberDTO);
        return toDTOs(memberTypeRepository.findByMember(member));
    }

    /* Get president of an association */
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

    /* Find MemberType by id and return DTO */
    public MemberTypeDTO findByIdDTO(long id) {
        return toDTO(memberTypeRepository.findById(id).orElseThrow());
    }

    /* Find all member type of an association */
    public Collection<MemberTypeDTO> findByAssociationIdDTO(Long associationId) {
    return associationService.findById(associationId)
        .getMemberTypes()
        .stream()
        .map(memberTypeMapper::toDTO)
        .collect(Collectors.toList());
    }

    /* Change member role in web controller */
    public void changeMemberRole(AssociationDTO associationDTO, MemberDTO requesterDTO, Long memberTypeId, String newRole) {

        // Retrieve the target MemberType to be updated
        MemberTypeDTO targetMemberTypeDTO = findByIdDTO(memberTypeId);

        // Check if the member type belongs to the specified association
        if (!targetMemberTypeDTO.association().id().equals(associationDTO.id())) {
            throw new IllegalArgumentException("Mismatched association and memberType");
        }

        // Check if the requester is the president of the association
        boolean requesterIsPresident = findByMemberDTO(requesterDTO).stream()
            .anyMatch(mt -> mt.association().id().equals(associationDTO.id())
                        && "president".equalsIgnoreCase(mt.name()));

        if (!requesterIsPresident) {
            throw new SecurityException("Only the president can change roles.");
        }

        // Check if the requester is trying to change their own role as president without promoting someone else first
        if (requesterDTO.id().equals(targetMemberTypeDTO.member().id())
            && "president".equalsIgnoreCase(targetMemberTypeDTO.name())) {
            throw new SecurityException("If you want to change your role, promote someone else first.");
        }

        // If the new role is president, demote the current president to a member
        if ("president".equalsIgnoreCase(newRole)) {

            List<MemberType> presidents = memberTypeRepository
                .findByAssociationIdAndNameIgnoreCase(associationDTO.id(), "president");

            for (MemberType president : presidents) {
                // If it's not the target, demote
                if (!targetMemberTypeDTO.member().id().equals(president.getMember().getId())){
                    president.setName("member");
                    save(president);
                }
            }
        }

        // Update the role of the target member type
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


    /* Method to permit to an user to leave an association */
    public void deleteUserFromAssociation(Long associationId, Long userId) {

        List<MemberType> typesToDelete = memberTypeRepository.findByMemberIdAndAssociationId(userId, associationId);

        // Check if the user is trying to leave as president
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

    /* Converted an association set to DTOs */
    private Collection<MemberTypeDTO> toDTOs(Collection<MemberType> memberType) {
        return memberTypeMapper.toDTOs(memberType);
    }

    /* Converted a DTO to entity */
    private MemberType toDomain(MemberTypeDTO memberTypeDTO){
        return memberTypeMapper.toDomain(memberTypeDTO);
    }
}

