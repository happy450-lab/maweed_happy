package com.example.demo;

import com.example.demo.domain.PushSubscription;
import com.example.demo.repository.PushSubscriptionRepository;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Security;
import java.util.List;

/**
 * ✅ PushNotificationService — بيبعت Web Push Notifications مجاناً
 * بيستخدم VAPID Keys للتشفير وبروتوكول Web Push القياسي
 */
@Service
public class PushNotificationService {

    @Autowired
    private PushSubscriptionRepository subscriptionRepository;

    @Value("${vapid.public.key}")
    private String vapidPublicKey;

    @Value("${vapid.private.key}")
    private String vapidPrivateKey;

    static {
        // تسجيل BouncyCastle كـ Security Provider (مطلوب للتشفير)
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * ✅ ابعت إشعار لمستخدم معين على كل أجهزته
     */
    public void sendToUser(String nationalId, String title, String body) {
        List<PushSubscription> subscriptions = subscriptionRepository.findByNationalId(nationalId);
        if (subscriptions.isEmpty()) return;

        for (PushSubscription sub : subscriptions) {
            try {
                sendPush(sub, title, body);
            } catch (Exception e) {
                System.err.println("⚠️ فشل إرسال Push لـ " + nationalId + ": " + e.getMessage());
                // لو الـ endpoint انتهى صلاحيته، احذفه
                if (e.getMessage() != null && e.getMessage().contains("410")) {
                    subscriptionRepository.deleteByEndpoint(sub.getEndpoint());
                }
            }
        }
    }

    private void sendPush(PushSubscription sub, String title, String body) throws Exception {
        PushService pushService = new PushService(vapidPublicKey, vapidPrivateKey);

        Subscription subscription = new Subscription(
            sub.getEndpoint(),
            new Subscription.Keys(sub.getP256dhKey(), sub.getAuthKey())
        );

        // بناء الرسالة بصيغة JSON
        String payload = String.format(
            "{\"title\":\"%s\",\"body\":\"%s\",\"icon\":\"/logomaweed.png\",\"dir\":\"rtl\"}",
            title.replace("\"", "\\\""),
            body.replace("\"", "\\\"")
        );

        Notification notification = new Notification(subscription, payload);
        pushService.send(notification);
    }
}
