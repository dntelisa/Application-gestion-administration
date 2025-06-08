package es.codeurjc.helloword_vscode.dto;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.codeurjc.helloword_vscode.model.Association;
import es.codeurjc.helloword_vscode.model.MemberType;
import es.codeurjc.helloword_vscode.model.Minute;

@Mapper(componentModel = "spring")
public interface AssociationMapper {

    @Mapping(target = "image", ignore = true)
    @Mapping(target = "memberTypes", ignore = true)
    @Mapping(target = "minutes", ignore = true)
    @Mapping(target = "members", ignore = true)
    Association toDomain(AssociationDTO dto);

    AssociationDTO toDTO(Association association);

    List<AssociationDTO> toDTOs(Collection<Association> associations);

    MemberTypeLightDTO toLightDTO(MemberType memberType);

    MinuteLightDTO toLightDTO(Minute minute);

    List<MemberTypeLightDTO> toLightDTOs(List<MemberType> memberTypes);

    List<MinuteLightDTO> toLightMinutes(List<Minute> minutes);
}

