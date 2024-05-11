package life.andre.sms487.auth;

import androidx.annotation.NonNull;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import life.andre.sms487.auth.errors.TokenException;
import life.andre.sms487.utils.DateUtil;

public class TokenData {
    @NonNull
    private final Claims payload;
    @NonNull
    private final Date expiresAt;

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

        payload = parsed.getPayload();

        expiresAt = payload.getExpiration();
        if (expiresAt == null || expiresAt.equals(DateUtil.ZERO_DATE)) {
            throw new TokenException("Expiration required", TokenException.TokenErrorType.NoExpiration);
        }
    }

    @NonNull
    public Claims getClaims() {
        return payload;
    }

    @NonNull
    public Date getExpiresAt() {
        return expiresAt;
    }
}
