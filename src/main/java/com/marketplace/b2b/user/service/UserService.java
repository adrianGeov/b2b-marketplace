package com.marketplace.b2b.user.service;

import com.marketplace.b2b.common.exception.ResourceNotFoundException;
import com.marketplace.b2b.user.dto.UserResponse;
import com.marketplace.b2b.user.mapper.UserMapper;
import com.marketplace.b2b.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));
    }
}