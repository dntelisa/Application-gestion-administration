package es.codeurjc.helloword_vscode.dto;

import es.codeurjc.helloword_vscode.model.Minute;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring", uses = {MemberMapper.class, AssociationBasicMapper.class})
public interface MinuteMapper {

    MinuteDTO toDTO(Minute minute);

    List<MinuteDTO> toDTOs(Collection<Minute> minutes);

    // Optionnel si tu veux du bidirectionnel
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "association", ignore = true)
    Minute toDomain(MinuteDTO minuteDTO);
}

