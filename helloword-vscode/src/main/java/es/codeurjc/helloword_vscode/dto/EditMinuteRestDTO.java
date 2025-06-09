package es.codeurjc.helloword_vscode.dto;

import java.util.List;

public record EditMinuteRestDTO(
    String date,
    List<Long> participantsIds,
    String content,
    double duration
) {}
