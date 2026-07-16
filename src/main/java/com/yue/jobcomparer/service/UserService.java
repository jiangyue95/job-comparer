package com.yue.jobcomparer.service;

import com.yue.jobcomparer.dto.UserResponse;
import com.yue.jobcomparer.entity.User;
import com.yue.jobcomparer.exception.InvalidAvatarException;
import com.yue.jobcomparer.repository.UserRepository;
import com.yue.jobcomparer.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final long MAX_AVATAR_BYTES =  1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png");

    private final UserRepository userRepository;
    private final FileStorage fileStorage;

    public UserResponse getByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        String avatarUrl = null;
        if (user.getAvatarKey() != null) {
            avatarUrl = fileStorage.generatePresignedUrl(user.getAvatarKey());
        }

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                avatarUrl,
                user.getCreatedAt()
        );
    }

    public void updateAvatar(String email, MultipartFile file) {

        if (file.isEmpty()) {
            throw new InvalidAvatarException("Avatar file is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidAvatarException("Only JPEG and PNG images are supported");
        }

        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new InvalidAvatarException("Avatar exceeds 1MB limit");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read avatar bytes for user={}", email, e);
            throw new InvalidAvatarException("Failed to read avatar file");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        String oldKey = user.getAvatarKey();

        String newKey = fileStorage.upload(content, file.getOriginalFilename(), contentType);

        user.setAvatarKey(newKey);
        userRepository.save(user);

        // The old object is deleted only after save() has commited. This method is
        // deliberately not @Transactional: an S3 delete cannot be rolled back, so running
        // it inside a transaction risks leaving the DB pointing at a deleted object.
        if (oldKey != null) {
            try {
                fileStorage.delete(oldKey);
            } catch (Exception e) {
                log.warn("Failed to delete old avatar key={}, user={}", oldKey, email, e);
            }
        }
    }
}
