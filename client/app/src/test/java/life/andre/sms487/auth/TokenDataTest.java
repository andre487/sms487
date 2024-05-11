package life.andre.sms487.auth;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.Date;

public class TokenDataTest extends TestCase {
    final private TestUtils utils = new TestUtils();

    final private Date tokenExpiresAt = new Date(1715473443000L);

    public void testBasic() throws IOException {
        String authKeyContent = this.utils.getResultseAsString("auth/auth_key.pub.pem");
        String tokenContent = this.utils.getResultseAsString("auth/auth_token.txt").trim();

        TokenData dt = new TokenData(authKeyContent, tokenContent);

        assertEquals(tokenExpiresAt, dt.getExpiresAt());
    }
}
