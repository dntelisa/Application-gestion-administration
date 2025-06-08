package es.codeurjc.helloword_vscode.dto;

import java.util.List;

public record NewMinuteRequestDTO(
    String date,
    List<Long> participantsIds,
    String content,
    double duration
) {}

