package es.codeurjc.helloword_vscode.dto;


public record MemberTypeDTO(
    Long id,
    String name,
    MemberDTO member,
    AssociationBasicDTO association
) {}
