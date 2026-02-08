package com.nhn.cloud.photoservice.service;

import com.nhn.cloud.photoservice.domain.album.Album;
import com.nhn.cloud.photoservice.domain.photo.Photo;
import com.nhn.cloud.photoservice.domain.user.User;
import com.nhn.cloud.photoservice.dto.request.AlbumCreateRequest;
import com.nhn.cloud.photoservice.dto.request.AlbumUpdateRequest;
import com.nhn.cloud.photoservice.dto.response.AlbumResponse;
import com.nhn.cloud.photoservice.exception.CustomException;
import com.nhn.cloud.photoservice.exception.ErrorCode;
import com.nhn.cloud.photoservice.repository.AlbumRepository;
import com.nhn.cloud.photoservice.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final UserRepository userRepository;
    private final Counter albumShareSuccessCounter;
    private final Counter albumShareFailureCounter;
    private final Timer albumListTimer;

    /**
     * 앨범 생성
     */
    @Transactional
    public AlbumResponse createAlbum(Long userId, AlbumCreateRequest request) {
        User user = getUserById(userId);

        Album album = Album.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        Album savedAlbum = albumRepository.save(album);
        log.info("Album created: {} by user: {}", savedAlbum.getId(), userId);

        return AlbumResponse.from(savedAlbum);
    }

    /**
     * 내 앨범 목록 조회
     */
    public List<AlbumResponse> getMyAlbums(Long userId) {
        return albumListTimer.record(() -> {
            User user = getUserById(userId);

            List<Album> albums = albumRepository.findByUserOrderByCreatedAtDesc(user);

            return albums.stream()
                    .map(AlbumResponse::from)
                    .collect(Collectors.toList());
        });
    }

    /**
     * 앨범 상세 조회
     */
    public AlbumResponse getAlbum(Long userId, Long albumId) {
        User user = getUserById(userId);

        Album album = albumRepository.findByIdAndUser(albumId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));

        return AlbumResponse.from(album);
    }

    /**
     * 공유 앨범 조회 (로그인 불필요)
     */
    public AlbumResponse getSharedAlbum(String shareToken) {
        Album album = albumRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));

        if (!album.getIsShared()) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Album is not shared");
        }

        return AlbumResponse.from(album);
    }

    /**
     * 앨범 수정
     */
    @Transactional
    public AlbumResponse updateAlbum(Long userId, Long albumId, AlbumUpdateRequest request) {
        User user = getUserById(userId);

        Album album = albumRepository.findByIdAndUser(albumId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));

        album.updateInfo(request.getTitle(), request.getDescription());

        log.info("Album updated: {} by user: {}", albumId, userId);

        return AlbumResponse.from(album);
    }

    /**
     * 앨범 공유 활성화
     */
    @Transactional
    public AlbumResponse enableSharing(Long userId, Long albumId) {
        User user = getUserById(userId);

        try {
            Album album = albumRepository.findByIdAndUser(albumId, user)
                    .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));

            album.enableSharing();
            albumShareSuccessCounter.increment();

            log.info("Album sharing enabled: {} by user: {}", albumId, userId);

            return AlbumResponse.from(album);
        } catch (Exception e) {
            albumShareFailureCounter.increment();
            throw e;
        }
    }

    /**
     * 앨범 공유 비활성화
     */
    @Transactional
    public AlbumResponse disableSharing(Long userId, Long albumId) {
        User user = getUserById(userId);

        Album album = albumRepository.findByIdAndUser(albumId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));

        album.disableSharing();

        log.info("Album sharing disabled: {} by user: {}", albumId, userId);

        return AlbumResponse.from(album);
    }

    /**
     * 앨범 삭제
     */
    @Transactional
    public void deleteAlbum(Long userId, Long albumId) {
        User user = getUserById(userId);

        Album album = albumRepository.findByIdAndUser(albumId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));

        albumRepository.delete(album);

        log.info("Album deleted: {} by user: {}", albumId, userId);
    }

    /**
     * 사용자 조회 헬퍼 메서드
     */
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public Album getAlbumByShareToken(String shareToken) {
        return albumRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));
    }

}