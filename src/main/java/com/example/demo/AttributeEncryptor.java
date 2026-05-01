package com.example.demo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Converter
@Component
public class AttributeEncryptor implements AttributeConverter<String, String> {

    private static final String AES = "AES";
    
    // مفتاح التشفير السري 32 بايت لضمان أعلى مستوى من الأمان (AES-256)
    // لا يمكن لأحد قراءة البيانات بدون هذا المفتاح
    private static final byte[] encryptionKey = "MaweedSuperSecretKeyForAES256Bit".getBytes();

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(AES);
            SecretKeySpec key = new SecretKeySpec(encryptionKey, AES);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException("فشل في تشفير البيانات", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(AES);
            SecretKeySpec key = new SecretKeySpec(encryptionKey, AES);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (Exception e) {
            // في حالة وجود بيانات قديمة غير مشفرة في قاعدة البيانات، نعرضها كما هي
            return dbData;
        }
    }
}
