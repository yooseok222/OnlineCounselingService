package kr.or.kosa.visang.domain.chat.controller;

import kr.or.kosa.visang.domain.chat.service.ChatService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@RequestMapping("/api/chat")
public class ChatExportController {

    private final ChatService chatService;

    public ChatExportController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/export/{roomId}")
    public ResponseEntity<Resource> downloadChatExport(@PathVariable("roomId") Long roomId) {
        String filePath = chatService.getExportPath(roomId);
        if (filePath == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        File file = new File(filePath);
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        Resource resource = new FileSystemResource(file);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition
                .attachment()
                .filename(file.getName())
                .build());
        headers.setContentType(MediaType.TEXT_PLAIN);

        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }
}
