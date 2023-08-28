package com.alex788.videos.entity;

import lombok.Getter;

import java.util.UUID;

@Getter
public class User {

    private final UUID id;

    public User() {
        this.id = UUID.randomUUID();
    }
}
