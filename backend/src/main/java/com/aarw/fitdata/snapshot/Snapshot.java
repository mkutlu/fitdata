package com.aarw.fitdata.snapshot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "snapshots")
@Getter
@Setter
public class Snapshot {

    @Id
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String data;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
