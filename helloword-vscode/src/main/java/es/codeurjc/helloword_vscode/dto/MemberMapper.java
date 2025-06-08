package es.codeurjc.helloword_vscode.dto;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.codeurjc.helloword_vscode.model.Member;
import es.codeurjc.helloword_vscode.model.MemberType;
import es.codeurjc.helloword_vscode.model.Minute;

@Mapper(componentModel = "spring")
public interface MemberMapper {

    @Mapping(source = "association.id", target = "associationId")
    @Mapping(source = "name", target = "memberType")
    AssociationMemberTypeDTO toRoleDTO(MemberType memberType);

    MinuteLightDTO toShortDTO(Minute minute);

    List<AssociationMemberTypeDTO> toRoleDTOs(List<MemberType> memberTypes);

    List<MinuteLightDTO> toShortMinutes(List<Minute> minutes);

    @Mapping(target = "memberTypes", ignore = true)
    @Mapping(target = "pwd", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "associations", ignore = true)
    @Mapping(target = "associationsWithRoles", ignore = true)
    @Mapping(target = "minutes", ignore = true)
    Member toDomain(MemberDTO dto);

    MemberDTO toDTO(Member member);

    List<MemberDTO> toDTOs(Collection<Member> members);
}

