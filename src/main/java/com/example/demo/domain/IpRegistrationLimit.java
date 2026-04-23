package com.example.demo.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 🔐 يتتبع عدد الحسابات المسجَّلة من كل IP
 * الحد الأقصى: 5 حسابات (مريض + طبيب) لكل IP
 */
@Entity
@Table(name = "ip_registration_limits")
public class IpRegistrationLimit {

    @Id
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(nullable = false)
    private int registrationCount = 0;

    @Column(nullable = false)
    private LocalDateTime firstRegisteredAt = LocalDateTime.now();

    @Column
    private LocalDateTime lastRegisteredAt;

    public IpRegistrationLimit() {}

    public IpRegistrationLimit(String ipAddress) {
        this.ipAddress = ipAddress;
        this.firstRegisteredAt = LocalDateTime.now();
        this.lastRegisteredAt  = LocalDateTime.now();
        this.registrationCount = 0;
    }

    // ─── Getters / Setters ────────────────────────────────────────────────

    public String getIpAddress()               { return ipAddress; }
    public void   setIpAddress(String ip)      { this.ipAddress = ip; }

    public int  getRegistrationCount()         { return registrationCount; }
    public void setRegistrationCount(int c)    { this.registrationCount = c; }

    public LocalDateTime getFirstRegisteredAt()                    { return firstRegisteredAt; }
    public void          setFirstRegisteredAt(LocalDateTime t)     { this.firstRegisteredAt = t; }

    public LocalDateTime getLastRegisteredAt()                     { return lastRegisteredAt; }
    public void          setLastRegisteredAt(LocalDateTime t)      { this.lastRegisteredAt = t; }
}
