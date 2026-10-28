package com.marketplace.b2b.user.mapper;

import com.marketplace.b2b.user.dto.UserResponse;
import com.marketplace.b2b.user.entity.User;
import org.mapstruct.Mapper;

/**
 * MapStruct genera la implementación en tiempo de compilación (target/generated-sources).
 * Al llamarse igual los campos, no se necesita configurar nada.
 */
@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);
}