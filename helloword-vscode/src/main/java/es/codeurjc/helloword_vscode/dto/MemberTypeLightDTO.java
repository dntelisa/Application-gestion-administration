package es.codeurjc.helloword_vscode.dto;


public record MemberTypeLightDTO(
    Long id,
    String name,
    MemberDTO member
) {}