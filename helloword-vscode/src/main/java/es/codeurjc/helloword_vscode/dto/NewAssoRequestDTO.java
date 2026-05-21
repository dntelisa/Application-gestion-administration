package es.codeurjc.helloword_vscode.dto;

import org.springframework.web.multipart.MultipartFile;

public record NewAssoRequestDTO (
        String name,
        MultipartFile imageField
){}
