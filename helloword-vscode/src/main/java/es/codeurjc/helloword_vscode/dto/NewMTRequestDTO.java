package es.codeurjc.helloword_vscode.dto;

public record NewMTRequestDTO (
    long memberId,
    long associationId,
    String memberType
) {}