package com.example.studio.auth;

import java.time.Instant;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SessionMapper {

    class Session {
        public String tokenHash;
        public Long userId;
        public Instant expiresAt;
    }

    @Insert("INSERT INTO user_sessions (token_hash, user_id, created_at, expires_at) "
            + "VALUES (#{tokenHash}, #{userId}, #{createdAt}, #{expiresAt})")
    void insert(@Param("tokenHash") String tokenHash, @Param("userId") long userId,
                @Param("createdAt") Instant createdAt, @Param("expiresAt") Instant expiresAt);

    @Select("SELECT token_hash, user_id, expires_at FROM user_sessions WHERE token_hash = #{tokenHash}")
    Session find(String tokenHash);

    @Delete("DELETE FROM user_sessions WHERE token_hash = #{tokenHash}")
    void delete(String tokenHash);
}
