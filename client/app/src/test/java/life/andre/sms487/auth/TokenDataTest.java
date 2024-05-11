package life.andre.sms487.auth;

import androidx.annotation.NonNull;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import junit.framework.TestCase;
import life.andre.sms487.auth.errors.TokenErrorType;
import life.andre.sms487.auth.errors.TokenException;
import life.andre.sms487.utils.DateUtil;
import life.andre.sms487.utils.ValueOrError;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TokenDataTest extends TestCase {
    @FunctionalInterface
    interface TokenFieldsBuilder {
        JwtBuilder call(JwtBuilder tokenBuilder) throws Exception;
    }

    static class TokenBuilder {
        @NonNull
        private final KeyPair keyPair;
        @NonNull
        private final String b64PubKey;

        public TokenBuilder() {
            keyPair = Jwts.SIG.ES512.keyPair().build();
            b64PubKey = createB64PublicKey(keyPair);
        }

        @NonNull
        public String build(TokenFieldsBuilder fieldBuilder) throws Exception {
            var jwsBuilder = fieldBuilder
                .call(Jwts.builder())
                .subject("Alice");
            return jwsBuilder.signWith(keyPair.getPrivate()).compact();
        }

        @NonNull
        public String build() throws Exception {
            return build((JwtBuilder x) -> x);
        }

        @NonNull
        public String getB64PubKey() {
            return b64PubKey;
        }

        @NonNull
        private static String createB64PublicKey(@NonNull KeyPair keyPair) {
            var val = keyPair.getPublic().getEncoded();
            return Base64.getEncoder().encodeToString(val);
        }
    }

    final private TestUtils utils = new TestUtils();
    final private TokenBuilder tokenBuilder = new TokenBuilder();

    final private Date tokenIssuedAt = new Date(1715387043000L);
    final private Date tokenNotBefore = new Date(1715387043000L);
    final private Date tokenExpiresAt = new Date(1715473443000L);

    public void testBasic() throws IOException {
        String authKeyContent = this.utils.getResultseAsString("auth/auth_key.pub.pem");
        String tokenContent = this.utils.getResultseAsString("auth/auth_token.txt").trim();

        var td = new TokenData(authKeyContent, tokenContent);

        assertTrue(td.getHeader().isPayloadEncoded());
        assertEquals("ES512", td.getHeader().getAlgorithm());

        assertEquals(tokenIssuedAt, td.getIssuedAt());
        assertEquals(tokenNotBefore, td.getNotBefore());
        assertEquals(tokenExpiresAt, td.getExpiresAt());
        assertEquals("test", td.getName());
        assertTrue(td.getAccess().sms());

        assertNull(td.getClaims().getAudience());
    }

    public void testAccess() throws Exception {
        var pk = tokenBuilder.getB64PubKey();
        String token;
        TokenData tokenData;

        // access is empty
        token = tokenBuilder.build(
            (JwtBuilder x) -> x
                .issuedAt(tokenIssuedAt)
                .notBefore(tokenNotBefore)
                .expiration(new Date(new Date().getTime() + 1000L))
                .claim("name", "test")
                .claim("access", new HashMap<String, Boolean>())
        );
        tokenData = new TokenData(pk, token);

        assertNotNull(tokenData.getAccess());
        assertFalse(tokenData.getAccess().sms());

        // access to SMS
        token = tokenBuilder.build(
            (JwtBuilder x) -> x
                .issuedAt(tokenIssuedAt)
                .notBefore(tokenNotBefore)
                .expiration(new Date(new Date().getTime() + 1000L))
                .claim("name", "test")
                .claim("access", Map.of("sms", true))
        );
        tokenData = new TokenData(pk, token);

        assertNotNull(tokenData.getAccess());
        assertTrue(tokenData.getAccess().sms());
    }

    public void testInvalidKey() throws Exception {
        String tokenContent = this.utils.getResultseAsString("auth/auth_token.txt").trim();

        // empty key
        try {
            new TokenData("", tokenContent);
            throw new Exception("There should be error");
        } catch (TokenException e) {
            assertEquals(TokenErrorType.InvalidKey, e.getErrorType());
        }

        // invalid key value
        try {
            new TokenData("1234567890", tokenContent);
            throw new Exception("There should be error");
        } catch (TokenException e) {
            assertEquals(TokenErrorType.InvalidKey, e.getErrorType());
        }

        // another key
        try {
            new TokenData(tokenBuilder.getB64PubKey(), tokenContent);
            throw new Exception("There should be error");
        } catch (TokenException e) {
            assertEquals(TokenErrorType.InvalidSignature, e.getErrorType());
        }
    }

    public void testInvalidToken() throws Exception {
        String authKeyContent = this.utils.getResultseAsString("auth/auth_key.pub.pem");

        // empty token
        try {
            new TokenData(authKeyContent, "");
            throw new Exception("There should be error");
        } catch (TokenException e) {
            assertEquals(TokenErrorType.InvalidToken, e.getErrorType());
        }

        // invalid token
        try {
            new TokenData(authKeyContent, "random string");
            throw new Exception("There should be error");
        } catch (TokenException e) {
            assertEquals(TokenErrorType.InvalidToken, e.getErrorType());
        }

        // invalid signature
        try {
            new TokenData(authKeyContent, tokenBuilder.build());
            throw new Exception("There should be error");
        } catch (TokenException e) {
            assertEquals(TokenErrorType.InvalidSignature, e.getErrorType());
        }
    }

    public void testIssuedAtErrors() throws Exception {
        var pk = tokenBuilder.getB64PubKey();
        String token;

        // no iss
        token = tokenBuilder.build();
        try {
            new TokenData(pk, token);
            throw new Exception("There should be error");
        } catch (TokenException e) {
            assertEquals(TokenErrorType.NoIssuedAt, e.getErrorType());
            assertEquals("iss is absent", e.getMessage());
        }

        // iss is zero date
        token = tokenBuilder.build((JwtBuilder x) -> x.issuedAt(DateUtil.ZERO_DATE));
        try {
            new TokenData(pk, token);
            throw new Exception("There should be error");
        } catch (TokenException e) {
            assertEquals(TokenErrorType.NoIssuedAt, e.getErrorType());
            assertEquals("iss is absent", e.getMessage());
        }

        // iss is after now
        token = tokenBuilder.build((JwtBuilder x) -> x.issuedAt(
            new Date(new Date().getTime() + 5000L)
        ));
        try {
            new TokenData(pk, token);
            throw new Exception("There should be error");
        } catch (TokenException e) {
            assertEquals(TokenErrorType.InvalidIssuedAt, e.getErrorType());
            assertEquals("issuedAt after now", e.getMessage());
        }
    }

    public void testNotBeforeErrors() throws Exception {
        var pk = tokenBuilder.getB64PubKey();
        String token;

        // no nbf
        token = tokenBuilder.build((JwtBuilder x) -> x.issuedAt(tokenIssuedAt));
        try {
            new TokenData(pk, token);
            throw new Exception("There should be error");
        } catch (TokenException e) {
            assertEquals(TokenErrorType.NoNotBefore, e.getErrorType());
            assertEquals("nbf is absent", e.getMessage());
        }

        // nbf is zero date
        token = tokenBuilder.build(
            (JwtBuilder x) -> x
                .issuedAt(tokenIssuedAt)
                .notBefore(DateUtil.ZERO_DATE)
        );
        try {
            new TokenData(pk, token);
            throw new Exception("There should be error");
        } catch (TokenException e) {
            assertEquals(TokenErrorType.NoNotBefore, e.getErrorType());
            assertEquals("nbf is absent", e.getMessage());
        }
    }

    public void testNameErrors() throws Exception {
        var pk = tokenBuilder.getB64PubKey();
        String token;

        // no name
        token = tokenBuilder.build(
            (JwtBuilder x) -> x
                .issuedAt(tokenIssuedAt)
                .notBefore(tokenNotBefore)
                .expiration(new Date(new Date().getTime() + 1000L))
        );
        try {
            new TokenData(pk, token);
            throw new Exception("There should be error");
        } catch (TokenException e) {
            assertEquals(TokenErrorType.NoName, e.getErrorType());
            assertEquals("name is absent", e.getMessage());
        }

        // name is empty
        token = tokenBuilder.build(
            (JwtBuilder x) -> x
                .issuedAt(tokenIssuedAt)
                .notBefore(tokenNotBefore)
                .expiration(new Date(new Date().getTime() + 1000L))
                .claim("name", "")
        );
        try {
            new TokenData(pk, token);
            throw new Exception("There should be error");
        } catch (TokenException e) {
            assertEquals(TokenErrorType.NoName, e.getErrorType());
            assertEquals("name is absent", e.getMessage());
        }
    }

    public void testAccessErrors() throws Exception {
        var pk = tokenBuilder.getB64PubKey();
        String token;

        // no access
        token = tokenBuilder.build(
            (JwtBuilder x) -> x
                .issuedAt(tokenIssuedAt)
                .notBefore(tokenNotBefore)
                .expiration(new Date(new Date().getTime() + 1000L))
                .claim("name", "test")
        );
        try {
            new TokenData(pk, token);
            throw new Exception("There should be error");
        } catch (TokenException e) {
            assertEquals(TokenErrorType.NoAccessField, e.getErrorType());
            assertEquals("access is absent", e.getMessage());
        }
    }

    public void testCreateMethod() throws IOException {
        String authKeyContent = this.utils.getResultseAsString("auth/auth_key.pub.pem");
        String tokenContent = this.utils.getResultseAsString("auth/auth_token.txt").trim();
        ValueOrError<TokenData, TokenException> td;

        // Success
        td = TokenData.create(authKeyContent, tokenContent);

        assertTrue(td.success());
        assertNull(td.getError());
        assertNotNull(td.getValue());
        assertEquals("test", td.getValueNonNull().getName());

        // Error
        td = TokenData.create("authKeyContent", tokenContent);

        assertFalse(td.success());
        assertNull(td.getValue());
        assertNotNull(td.getError());

        assertEquals(TokenErrorType.InvalidKey, td.getErrorNonNull().getErrorType());
    }
}
