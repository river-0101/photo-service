package com.nhn.cloud.photoservice.repository;

import com.nhn.cloud.photoservice.domain.album.Album;
import com.nhn.cloud.photoservice.domain.photo.Photo;
import com.nhn.cloud.photoservice.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByUserOrderByCreatedAtDesc(User user);

    List<Photo> findByAlbumOrderByCreatedAtDesc(Album album);

    Optional<Photo> findByIdAndUser(Long id, User user);

    List<Photo> findByUserAndAlbumIsNullOrderByCreatedAtDesc(User user);
}