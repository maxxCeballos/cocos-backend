package com.cocos.portfolio_service.user.infrastructure.persistence;

import com.cocos.portfolio_service.user.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUserDomain(UserEntity userEntity);
}
