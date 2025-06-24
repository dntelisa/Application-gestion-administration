package es.codeurjc.helloword_vscode.dto;

import java.util.List;
import java.util.Collection;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.codeurjc.helloword_vscode.model.MemberType;


@Mapper(componentModel = "spring")
public interface AssociationMemberTypeMapper {

    @Mapping(source = "association.id", target = "associationId")
    @Mapping(source = "association.name", target = "associationName")
    @Mapping(source = "name", target = "memberType")
    AssociationMemberTypeDTO toDTO(MemberType memberType);

    List<AssociationMemberTypeDTO> toDTOs(Collection<MemberType> memberTypes);

    @Mapping(source = "association", target = "association", ignore = false)
    @Mapping(source = "member", target = "member", ignore = false)
    @Mapping(target = "association.imageFile", ignore = true)
    @Mapping(target = "association.image", ignore = true)
    @Mapping(target = "association.imagePath", ignore = true)
    @Mapping(target = "association.memberTypes", ignore = true)
    @Mapping(target = "association.minutes", ignore = true)
    @Mapping(target = "member.pwd", ignore = true)
    @Mapping(target = "member.roles", ignore = true)
    @Mapping(target = "member.memberTypes", ignore = true)
    @Mapping(target = "member.associations", ignore = true)
    @Mapping(target = "member.associationsWithRoles", ignore = true)
    @Mapping(target = "member.minutes", ignore = true)
    MemberType toDomain(MemberTypeDTO dto);
}

