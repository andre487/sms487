package life.andre.sms487.auth;

import androidx.annotation.NonNull;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import life.andre.sms487.auth.errors.TokenErrorType;
import life.andre.sms487.auth.errors.TokenException;
import life.andre.sms487.utils.DateUtil;
import life.andre.sms487.utils.ValueOrError;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class TokenData {
    @JsonRootName("access")
    public static class AccessField {
        @JsonProperty("sms")
        private boolean sms;

        private final  Map<String, String> properties = new HashMap<>();

        @JsonProperty("sms")
        public boolean sms() {
            return sms;
        }

        @JsonAnySetter
        public void setOtherProperty(String key, String value) {
            properties.put(key, value);
        }

        /** @noinspection unused*/
        public Map<String, String> getProperties() {
            return properties;
        }
    }

    @NonNull
    public static ValueOrError<TokenData, TokenException> create(@NonNull String publicKey, @NonNull String token) {
        try {
            return new ValueOrError<>(new TokenData(publicKey, token));
        } catch (TokenException e) {
            return new ValueOrError<>(e);
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
            throw new TokenException(e.toString(), e, TokenErrorType.InvalidKey);
        }

        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            throw new TokenException(e.toString(), e, TokenErrorType.InternalError);
        }

        PublicKey pubKey;
        try {
            pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
        } catch (InvalidKeySpecException e) {
            throw new TokenException(e.toString(), e, TokenErrorType.InvalidKey);
        }

        Jws<Claims> parsed;
        try {
            parsed = Jwts.parser()
                .verifyWith(pubKey)
                .build()
                .parseSignedClaims(token);
        } catch (SignatureException e) {
            throw new TokenException(e.toString(), e, TokenErrorType.InvalidSignature);
        } catch (ExpiredJwtException e) {
            throw new TokenException(e.toString(), e, TokenErrorType.TokenExpired);
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenException(e.toString(), e, TokenErrorType.InvalidToken);
        }

        header = parsed.getHeader();
        payload = parsed.getPayload();

        issuedAt = payload.getIssuedAt();
        checkDateField("iss", issuedAt, TokenErrorType.NoIssuedAt);
        if (issuedAt.after(new Date())) {
            throw new TokenException("issuedAt after now", TokenErrorType.InvalidIssuedAt);
        }

        notBefore = payload.getNotBefore();
        checkDateField("nbf", notBefore, TokenErrorType.NoNotBefore);

        expiresAt = payload.getExpiration();
        checkDateField("exp", expiresAt, TokenErrorType.NoExpiration);

        try {
            name = (String) Objects.requireNonNull(payload.getOrDefault("name", ""));
        } catch (NullPointerException e) {
            throw new TokenException(e.toString(), e, TokenErrorType.InternalError);
        }
        if (name.isEmpty()) {
            throw new TokenException("name is absent", TokenErrorType.NoName);
        }

        var accessObj = payload.getOrDefault("access", null);
        if (accessObj == null) {
            throw new TokenException("access is absent", TokenErrorType.NoAccessField);
        }

        try {
            access = Objects.requireNonNull(new ObjectMapper().convertValue(accessObj, AccessField.class));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new TokenException("access is absent", TokenErrorType.InvalidAccessField);
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

    private void checkDateField(@NonNull String fieldName, Date val, TokenErrorType errType) {
        if (val == null || val.equals(DateUtil.ZERO_DATE)) {
            throw new TokenException(fieldName + " is absent", errType);
        }
    }

    @NonNull
    public String getName() {
        return name;
    }
}
