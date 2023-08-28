package com.alex788.videos.entity;

import java.util.UUID;

public record Video(UUID userId, String name) { // todo: invariants
}
