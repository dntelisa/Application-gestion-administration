package es.codeurjc.helloword_vscode.dto;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;

import es.codeurjc.helloword_vscode.model.Association;

@Mapper(componentModel = "spring")
public interface AssociationBasicMapper {
    AssociationBasicDTO toDTO(Association association);

    List<AssociationBasicDTO> toDTOs(Collection<Association> associations);    

    AssociationBasicDTO toDomain(AssociationDTO associationDTO);
}
