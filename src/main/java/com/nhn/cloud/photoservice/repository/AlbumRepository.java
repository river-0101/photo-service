package com.nhn.cloud.photoservice.repository;

import com.nhn.cloud.photoservice.domain.album.Album;
import com.nhn.cloud.photoservice.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {

    List<Album> findByUserOrderByCreatedAtDesc(User user);

    Optional<Album> findByShareToken(String shareToken);

    Optional<Album> findByIdAndUser(Long id, User user);
}