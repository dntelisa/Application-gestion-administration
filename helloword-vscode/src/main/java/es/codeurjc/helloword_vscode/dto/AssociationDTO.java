package es.codeurjc.helloword_vscode.dto;

import java.sql.Blob;
import java.util.List;

public record AssociationDTO (
    Long id,
    String name,
    boolean image,
    List<MemberTypeDTO> memberTypes,
    List<MinuteDTO> minutes
){}
