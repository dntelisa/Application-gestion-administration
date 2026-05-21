package es.codeurjc.helloword_vscode.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import es.codeurjc.helloword_vscode.ResourceNotFoundException;
import es.codeurjc.helloword_vscode.dto.AssociationMemberTypeDTO;
import es.codeurjc.helloword_vscode.dto.AssociationMemberTypeMapper;
import es.codeurjc.helloword_vscode.dto.MemberDTO;
import es.codeurjc.helloword_vscode.dto.MemberDetailsDTO;
import es.codeurjc.helloword_vscode.dto.MemberMapper;
import es.codeurjc.helloword_vscode.dto.MinuteLightDTO;
import es.codeurjc.helloword_vscode.dto.NewMemberRequestDTO;
import es.codeurjc.helloword_vscode.dto.PagedResponseDTO;
import es.codeurjc.helloword_vscode.model.Association;
import es.codeurjc.helloword_vscode.model.Member;
import es.codeurjc.helloword_vscode.model.MemberType;
import es.codeurjc.helloword_vscode.model.Minute;
import es.codeurjc.helloword_vscode.repository.MemberRepository;
import es.codeurjc.helloword_vscode.repository.MinuteRepository;

/*
 * This service class provides methods to perform various operations on Member entities,
 * such as saving, retrieving, and deleting users. It implements UserDetailsService to load user-specific 
 * data
*/
@Service
public class MemberService implements UserDetailsService {

    // Autowired repositories for database interactions //

    @Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberTypeService memberTypeService;

	@Autowired
	private MinuteRepository minuteRepository;

	@Autowired
    private PasswordEncoder passwordEncoder;

	@Autowired
	private AssociationMemberTypeMapper associationMemberTypeMapper;

	@Autowired
	private MemberMapper memberMapper;

	@Autowired
	private AssociationService associationService;

	/* Save user */
    public void save(Member member) {
		memberRepository.save(member);
	}

	/* Create User */
	public MemberDTO createMember(NewMemberRequestDTO memberDTO) {
		try {
			MemberDTO existingMember = findByNameDTO(memberDTO.name());
			if (existingMember != null) {
				throw new IllegalArgumentException("This username already exists");
			}
		} catch (ResourceNotFoundException e) {
			// If no member is found, continue with member creation
		}

		// By default, a new user is necessarily user
		Member member = new Member(
			memberDTO.name(),
			memberDTO.surname(),
			passwordEncoder.encode(memberDTO.password()),
			"USER"
		);

		save(member);

		return toDTO(member);
	}



	/* Find user by their name */
	public Member findByName(String name) {
		return memberRepository.findByName(name).orElseThrow(() -> new ResourceNotFoundException("User not found: " + name));
	}

	/* Find user by their name and return DTO */
	public MemberDTO findByNameDTO(String name) {
		return toDTO(
			memberRepository.findByName(name)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + name))
		);
	}


	/* Load data of user by finding them by their username */
    @Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// Retrieve the user by username
		Member member = memberRepository.findByName(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		// Map the user's roles to granted authorities
		List<GrantedAuthority> roles = new ArrayList<>();
		for (String role : member.getRoles()) {
			roles.add(new SimpleGrantedAuthority("ROLE_" + role));
		}

		// Return a UserDetails object containing the user's data
		return new org.springframework.security.core.userdetails.User(member.getName(), 
				member.getPwd(), roles);
	}


	/* Find user by ID */
	public Member findById(long id) {
		return memberRepository.findById(id) .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
	}

	/* Find user by ID and return DTO*/
	public MemberDTO findByIdDTO(long id) {
		return toDTO(memberRepository.findById(id).orElseThrow());
	}

	/* All details of an user */
	public MemberDetailsDTO findDetailsById(Long id) {
		Member member = memberRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Member not found"));


		List<AssociationMemberTypeDTO> roles = associationMemberTypeMapper.toDTOs(member.getMemberTypes());
		List<MinuteLightDTO> minutes = memberMapper.toShortMinutes(member.getMinutes());

		return new MemberDetailsDTO(
			member.getId(),
			member.getName(),
			member.getSurname(),
			roles,
			minutes
		);
	}

	/* Update user */
	public void updateUserDTO(String currentUsername, NewMemberRequestDTO dto) {
		Member member = findByName(currentUsername);

		if (!member.getName().equals(dto.name())) {
			try {
				findByName(dto.name());
				// Si on arrive ici = username existe déjà
				throw new IllegalArgumentException("This username already exists");
			} catch (ResourceNotFoundException e) {
				// Si exception = c'est OK = username n'existe pas encore
			}
		}

		member.setName(dto.name());
		member.setSurname(dto.surname());

		if (dto.password() != null && !dto.password().isBlank()) {
			member.setPwd(passwordEncoder.encode(dto.password()));
		}

		save(member);
	}


	/* Update user by id and return DTO */
	public MemberDTO updateUserIdDTO(Long id, NewMemberRequestDTO dto, Authentication authentication) {
		Member member = findById(id);

		String loggedUsername = authentication.getName();
		boolean isAdmin = authentication.getAuthorities().stream()
				.anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

		if (!member.getName().equalsIgnoreCase(loggedUsername) && !isAdmin) {
			throw new SecurityException("You are not allowed to update this profile.");
		}

		if (!member.getName().equals(dto.name())) {
			try {
				findByName(dto.name());
				throw new IllegalArgumentException("This username already exists");
			} catch (ResourceNotFoundException e) {
				// OK: username n’existe pas encore
			}
		}

		member.setName(dto.name());
		member.setSurname(dto.surname());

		if (dto.password() != null && !dto.password().isBlank()) {
			member.setPwd(passwordEncoder.encode(dto.password()));
		}

		save(member);
		return toDTO(member);
	}


	/* Delete user */
	public void delete(Member member) throws IOException {
		// Retrieve user by ID
		Member user = member;

		// 1. Delete member type in association
		List<MemberType> memberTypes = memberTypeService.findByMember(user);
		for (MemberType memberType : memberTypes) {
			memberTypeService.delete(memberType);
		}

		// 2. Delete participation to meetings
		List<Minute> minutes = minuteRepository.findAllByParticipantId(member.getId());
		for (Minute minute : minutes) {
			minute.getParticipants().remove(member);
			minuteRepository.save(minute);
		}

		// 3. Delete user
		memberRepository.delete(user);
	}


	/* Delete user by id and return DTO */
	public MemberDTO deleteMemberDTO(Long memberId) throws IOException {
		// Retrieve the complete entity
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		// Convert to DTO before deletion to avoid lazy loading issues later
		MemberDTO memberDTO = toDTO(member);

		// Delete member types in associations
		List<MemberType> memberTypes = memberTypeService.findByMember(member);
		for (MemberType memberType : memberTypes) {
			memberTypeService.delete(memberType);
		}

		// Remove participation in meetings
		List<Minute> minutes = minuteRepository.findAllByParticipantId(member.getId());
		for (Minute minute : minutes) {
			minute.getParticipants().remove(member);
			minuteRepository.save(minute);
		}

		memberRepository.delete(member);

		return memberDTO;
	}

	/* Method that permit to an user to delete his own account and return DTO */
	public MemberDTO deleteMemberDTO(Long memberId, Authentication authentication) throws IOException {
		
		// Retrieve the complete entity
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		// Authorization check: only the user himself or an admin can delete
		String loggedUsername = authentication.getName();

		boolean isAdmin = authentication.getAuthorities().stream()
			.anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

		if (!member.getName().equalsIgnoreCase(loggedUsername) && !isAdmin) {
			throw new SecurityException("You are not allowed to delete this profile.");
		}

		// Convert to DTO before deletion to avoid lazy loading issues later
		MemberDTO memberDTO = toDTO(member);

		// Delete member types in associations
		List<MemberType> memberTypes = memberTypeService.findByMember(member);
		for (MemberType memberType : memberTypes) {
			memberTypeService.delete(memberType);
		}

		// Remove participation in meetings
		List<Minute> minutes = minuteRepository.findAllByParticipantId(member.getId());
		for (Minute minute : minutes) {
			minute.getParticipants().remove(member);
			minuteRepository.save(minute);
		}

		memberRepository.delete(member);

		return memberDTO;
	}

	/* Find all members of an association */
	public List<MemberDTO> findMembersByAssociationId(Long associationId) {
		List<Member> members = memberRepository.findMembersByAssociationId(associationId);
		return members.stream().map(this::toDTO).toList();
	}

	/* Get all members with pagination */
	public PagedResponseDTO<MemberDTO> getPagedMembers(Pageable pageable) {
		Page<Member> page = memberRepository.findAll(pageable);

		List<MemberDTO> dtoList = page.getContent().stream()
			.map(memberMapper::toDTO)
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
	private MemberDTO toDTO(Member member) {
		return memberMapper.toDTO(member);
	}

	/* Converted a member to DTO */
	public MemberDTO toDTO(MemberDetailsDTO details) {
		return new MemberDTO(details.id(), details.name(), details.surname());
	}

	// /* Converted a member set to DTOs */
	// private Collection<MemberDTO> toDTOs(Collection<Member> members) {
	// 	return memberMapper.toDTOs(members);
	// }

	// /* Converted a DTO to entity */
	// private Member toDomain(MemberDTO memberDTO){
	// 	return memberMapper.toDomain(memberDTO);
	// }
}
