package com.game.buildingstory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class GameEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    @Column(nullable = false)
    private String eventKey;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(nullable = false)
    private String imageLabel;

    @Column(nullable = false)
    private String effectKey;

    @Enumerated(EnumType.STRING)
    private GameEventStatus status;

    protected GameEvent() {
    }

    public GameEvent(Player player, GameEventDefinition definition) {
        this.player = player;
        this.eventKey = definition.key();
        this.title = definition.title();
        this.body = definition.body();
        this.imageLabel = definition.imageLabel();
        this.effectKey = definition.effectKey();
        this.status = GameEventStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public String getEventKey() {
        return eventKey;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getImageLabel() {
        return imageLabel;
    }

    public String getEffectKey() {
        return effectKey;
    }

    public GameEventStatus getStatus() {
        return status;
    }

    public void complete() {
        this.status = GameEventStatus.COMPLETED;
    }
}
