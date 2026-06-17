package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class SecretaryTenantEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private OwnedBuilding building;

    private String secretaryKey;
    private String city;
    private Integer startedDayCount;
    private Integer acceptedDayCount;
    private Integer dueDayCount;

    @Enumerated(EnumType.STRING)
    private SecretaryTenantEventStatus status;

    protected SecretaryTenantEvent() {
    }

    public SecretaryTenantEvent(Player player, OwnedBuilding building, String secretaryKey, String city, int startedDayCount) {
        this.player = player;
        this.building = building;
        this.secretaryKey = secretaryKey;
        this.city = city;
        this.startedDayCount = startedDayCount;
        this.status = SecretaryTenantEventStatus.TENANT;
    }

    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public OwnedBuilding getBuilding() {
        return building;
    }

    public String getSecretaryKey() {
        return secretaryKey;
    }

    public String getCity() {
        return city;
    }

    public int getStartedDayCount() {
        return startedDayCount == null ? 1 : startedDayCount;
    }

    public int getAcceptedDayCount() {
        return acceptedDayCount == null ? 0 : acceptedDayCount;
    }

    public int getDueDayCount() {
        return dueDayCount == null ? 0 : dueDayCount;
    }

    public SecretaryTenantEventStatus getStatus() {
        return status;
    }

    public boolean isActiveTenant() {
        return status != SecretaryTenantEventStatus.COMPLETED;
    }

    public void makeTenant() {
        this.acceptedDayCount = null;
        this.dueDayCount = null;
        this.status = SecretaryTenantEventStatus.TENANT;
    }

    public void makeRequestAvailable() {
        this.status = SecretaryTenantEventStatus.REQUEST_AVAILABLE;
    }

    public void acceptRequest(int currentDayCount, int durationDays) {
        this.acceptedDayCount = currentDayCount;
        this.dueDayCount = currentDayCount + durationDays;
        this.status = durationDays == 0 ? SecretaryTenantEventStatus.HIRE_AVAILABLE : SecretaryTenantEventStatus.REQUEST_ACCEPTED;
    }

    public void makeHireAvailable() {
        this.status = SecretaryTenantEventStatus.HIRE_AVAILABLE;
    }

    public void complete() {
        this.status = SecretaryTenantEventStatus.COMPLETED;
    }
}
