package com.example.studio.auth;

import java.time.Instant;

/** users 表行对象（MyBatis 通过字段直接映射）。 */
public class User {
    public Long id;
    public String email;
    public String passwordHash;
    public String displayName;
    public Instant createdAt;
}
