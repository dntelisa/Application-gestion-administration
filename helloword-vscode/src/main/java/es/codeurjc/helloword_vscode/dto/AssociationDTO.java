package es.codeurjc.helloword_vscode.dto;

import java.util.List;

public record AssociationDTO (
    Long id,
    String name,
    boolean image,
    List<MemberTypeLightDTO> memberTypes,
    List<MinuteLightDTO> minutes
){}
