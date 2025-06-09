package es.codeurjc.helloword_vscode.dto;

import java.util.List;

public record EditMinuteRequestDTO(
    Long minuteId,
    Long assoId,
    String date,
    List<Long> participantsIds,
    String content,
    double duration
) {}

