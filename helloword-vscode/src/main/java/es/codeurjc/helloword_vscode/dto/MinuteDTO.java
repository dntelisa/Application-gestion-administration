package es.codeurjc.helloword_vscode.dto;

import java.util.List;

public record MinuteDTO (
     Long id,
     String date,
     List<MemberDTO> participants,
     String content,
     Double duration,
     AssociationBasicDTO association
) {}


