package com.rsvp.common.util;

import com.rsvp.common.config.RsvpProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class EncryptionService {

	private static final int IV_LENGTH = 12;
	private static final int TAG_BITS = 128;

	private final SecretKeySpec key;
	private final SecureRandom random = new SecureRandom();

	public EncryptionService(RsvpProperties properties) {
		byte[] decoded;
		try {
			decoded = Base64.getDecoder().decode(properties.getEncryption().getKey());
		} catch (IllegalArgumentException ex) {
			throw new IllegalStateException("ENCRYPTION_KEY must be valid base64");
		}
		if (decoded.length != 32) {
			throw new IllegalStateException("ENCRYPTION_KEY must decode to 32 bytes");
		}
		this.key = new SecretKeySpec(decoded, "AES");
	}

	public String encrypt(String plaintext) {
		if (plaintext == null) {
			return null;
		}
		try {
			byte[] iv = new byte[IV_LENGTH];
			random.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
			ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
			buffer.put(iv);
			buffer.put(encrypted);
			return Base64.getEncoder().encodeToString(buffer.array());
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Unable to encrypt secret", ex);
		}
	}

	public String decrypt(String ciphertext) {
		if (ciphertext == null || ciphertext.isBlank()) {
			return null;
		}
		try {
			byte[] payload = Base64.getDecoder().decode(ciphertext);
			ByteBuffer buffer = ByteBuffer.wrap(payload);
			byte[] iv = new byte[IV_LENGTH];
			buffer.get(iv);
			byte[] encrypted = new byte[buffer.remaining()];
			buffer.get(encrypted);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException | IllegalArgumentException ex) {
			throw new IllegalStateException("Unable to decrypt secret", ex);
		}
	}
}
