package es.codeurjc.helloword_vscode.dto;

import java.util.List;

public record MemberDetailsDTO(
    Long id,
    String name,
    String surname,
    List<AssociationMemberTypeDTO> roles,
    List<MinuteLightDTO> participatedMinutes
) {}
