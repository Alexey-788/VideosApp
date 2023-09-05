package com.alex788.videos.controller;

import com.alex788.videos.entity.Video;
import com.alex788.videos.entity.VideoInfo;
import com.alex788.videos.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @GetMapping("/has_video")
    public String hasVideo(@RequestParam String userName, @RequestParam String videoName) {
        boolean hasVideo = videoService.doesUserHaveVideoWithName(videoName, UUID.fromString(userName));
        String s = hasVideo ? "has" : "hasn't";
        return "User " + userName + " " + s + " video " + videoName + ".";
    }

    @GetMapping("/get_video_infos")
    public List<VideoInfo> getVideoInfos(@RequestParam String userName) {
        return videoService.getAllVideoInfosByUserId(UUID.fromString(userName));
    }

    @GetMapping(value = "/download_video")
    public ResponseEntity<Resource> downloadVideo(@RequestParam String userName, @RequestParam String videoName) throws IOException {
        Video video = videoService.getVideo(videoName, UUID.fromString(userName));

        int available = video.inputStream().available();
        InputStreamResource inputStreamResource = new InputStreamResource(video.inputStream());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("filename", video.name());
        httpHeaders.setContentDispositionFormData("attachment", video.name());
        return ResponseEntity.ok()
                .contentLength(available)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .headers(httpHeaders)
                .body(inputStreamResource);
    }

    @PostMapping(value = "/save_video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String saveVideo(@RequestParam("file") MultipartFile file, @RequestParam String userName, @RequestParam String videoName) throws IOException {
        Video video = new Video(UUID.fromString(userName), videoName, file.getInputStream());
        videoService.save(video);
        return "OK";
    }
}
