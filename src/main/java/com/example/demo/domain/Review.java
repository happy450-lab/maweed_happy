package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String doctorNationalId;

    @Column(nullable = false)
    private String patientNationalId;

    @Column(nullable = false)
    private String patientName;

    @Column
    private String doctorName;

    @Column(nullable = false)
    private Integer rating; // 1 to 5

    @Column(columnDefinition = "TEXT")
    private String comment; // review description

    private LocalDateTime createdAt = LocalDateTime.now();
}
