package es.codeurjc.helloword_vscode.dto;

/**
    This class is a Data Transfer Object (DTO) used to transfer data between software application subsystems.
    It encapsulates the association and the type of membership a user has within that association.
    It is particularly useful for transferring association membership information to the frontend or other services.
**/

public record AssociationMemberTypeDTO(
    Long associationId,
    String associationName,
    String memberType
) {}

