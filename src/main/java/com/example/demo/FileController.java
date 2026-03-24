package com.example.demo;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@CrossOrigin(origins = {"http://localhost:3000", "https://maweed-ui.vercel.app"})
@RestController
@RequestMapping("/uploads")
public class FileController {

    @GetMapping("/photos/{filename}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String filename) {
        return serveFile("uploads/photos", filename);
    }

    @GetMapping("/covers/{filename}")
    public ResponseEntity<Resource> getCover(@PathVariable String filename) {
        return serveFile("uploads/covers", filename);
    }

    @GetMapping("/certificates/{filename}")
    public ResponseEntity<Resource> getCertificate(@PathVariable String filename) {
        return serveFile("uploads/certificates", filename);
    }

    private ResponseEntity<Resource> serveFile(String directory, String filename) {
        try {
            Path filePath = Paths.get(directory).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
