package life.andre.sms487.auth;

import androidx.annotation.NonNull;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import life.andre.sms487.auth.errors.TokenException;
import life.andre.sms487.utils.DateUtil;

public class TokenData {
    @JsonRootName("access")
    public static class AccessField {
        @JsonProperty("sms")
        private boolean sms;

        @JsonProperty("sms")
        public boolean sms() {
            return sms;
        }
    }

    @NonNull
    private final JwsHeader header;
    @NonNull
    private final Claims payload;
    @NonNull
    private final Date issuedAt;
    @NonNull
    private final Date notBefore;
    @NonNull
    private final Date expiresAt;
    @NonNull
    private final String name;
    @NonNull
    private final AccessField access;

    TokenData(@NonNull String publicKey, @NonNull String token) throws TokenException {
        var trimmedKey = publicKey
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .trim();

        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(trimmedKey);
        } catch (IllegalArgumentException e) {
            throw new TokenException(e.toString(), e, TokenException.TokenErrorType.InvalidKey);
        }

        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            throw new TokenException(e.toString(), e, TokenException.TokenErrorType.InternalError);
        }

        PublicKey pubKey;
        try {
            pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
        } catch (InvalidKeySpecException e) {
            throw new TokenException(e.toString(), e, TokenException.TokenErrorType.InvalidKey);
        }

        Jws<Claims> parsed;
        try {
            parsed = Jwts.parser()
                .verifyWith(pubKey)
                .build()
                .parseSignedClaims(token);
        } catch (JwtException e) {
            throw new TokenException(e.toString(), e, TokenException.TokenErrorType.InvalidToken);
        }

        header = parsed.getHeader();
        payload = parsed.getPayload();

        issuedAt = payload.getIssuedAt();
        checkDateField(issuedAt, TokenException.TokenErrorType.NoIssuedAt);
        if (issuedAt.after(new Date())) {
            throw new TokenException("issuedAt after now", TokenException.TokenErrorType.InvalidIssuedAt);
        }

        notBefore = payload.getNotBefore();
        checkDateField(notBefore, TokenException.TokenErrorType.NoNotBefore);

        expiresAt = payload.getExpiration();
        checkDateField(expiresAt, TokenException.TokenErrorType.NoExpiration);

        try {
            name = (String) Objects.requireNonNull(payload.getOrDefault("name", ""));
        } catch (NullPointerException e) {
            throw new TokenException(e.toString(), e, TokenException.TokenErrorType.InternalError);
        }
        if (name.isEmpty()) {
            throw new TokenException("name is absent", TokenException.TokenErrorType.NoName);
        }

        var accessObj = payload.getOrDefault("access", null);
        if (accessObj == null) {
            throw new TokenException("access is absent", TokenException.TokenErrorType.NoAccessField);
        }

        try {
            access = Objects.requireNonNull(new ObjectMapper().convertValue(accessObj, AccessField.class));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new TokenException("access is absent", TokenException.TokenErrorType.InvalidAccessField);
        }
    }

    @NonNull
    public JwsHeader getHeader() {
        return header;
    }

    @NonNull
    public Claims getClaims() {
        return payload;
    }

    @NonNull
    public Date getIssuedAt() {
        return issuedAt;
    }

    @NonNull
    public Date getNotBefore() {
        return notBefore;
    }

    @NonNull
    public Date getExpiresAt() {
        return expiresAt;
    }

    @NonNull
    public AccessField getAccess() {
        return access;
    }

    private void checkDateField(Date val, TokenException.TokenErrorType errType) {
        if (val == null || val.equals(DateUtil.ZERO_DATE)) {
            throw new TokenException("Expiration required", errType);
        }
    }

    @NonNull
    public String getName() {
        return name;
    }
}
