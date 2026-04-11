package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;

/**
 * ✅ PushSubscription — بتحفظ بيانات الـ Browser Subscription لكل مستخدم
 * المتصفح بيديك endpoint + مفتاحين تشفير لما توافق على الإشعارات
 */
@Entity
@Table(name = "push_subscriptions")
@Data
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // الرقم القومي للمستخدم (مريض أو دكتور)
    @Column(nullable = false)
    private String nationalId;

    // الـ URL الخاص بالمتصفح (مختلف لكل جهاز/متصفح)
    @Column(nullable = false, length = 1024)
    private String endpoint;

    // مفاتيح التشفير — مطلوبة لتشفير الرسالة
    @Column(nullable = false, length = 512)
    private String p256dhKey;

    @Column(nullable = false, length = 256)
    private String authKey;
}
