package es.codeurjc.helloword_vscode.dto;

public record MinuteLightDTO (
     Long id,
     String date,
     String content,
     Double duration
) {}
