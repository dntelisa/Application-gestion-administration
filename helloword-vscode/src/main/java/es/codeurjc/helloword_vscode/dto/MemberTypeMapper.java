package es.codeurjc.helloword_vscode.dto;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.codeurjc.helloword_vscode.model.MemberType;

@Mapper(componentModel = "spring", uses = {MemberMapper.class, AssociationMapper.class})
public interface MemberTypeMapper {

    @Mapping(target = "association", ignore = true)
    @Mapping(source = "member", target = "member")
    MemberTypeDTO toDTO(MemberType memberType);

    List<MemberTypeDTO> toDTOs(Collection<MemberType> memberTypes);
    
    @Mapping(source = "association", target = "association")
    @Mapping(source = "member", target = "member")
    @Mapping(target = "association.image", ignore = true)
    @Mapping(target = "association.imageFile", ignore = true)
    @Mapping(target = "association.memberTypes", ignore = true)
    @Mapping(target = "association.minutes", ignore = true)
    @Mapping(target = "association.members", ignore = true)
    MemberType toDomain(MemberTypeDTO dto);
}

