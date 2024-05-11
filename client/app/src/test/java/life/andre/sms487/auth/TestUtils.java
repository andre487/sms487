package life.andre.sms487.auth;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Objects;

public class TestUtils {
    @NonNull
    public String getResultseAsString(@NonNull String resPath) throws IOException {
        return new String(
            Objects
                .requireNonNull(
                    this.getClass()
                        .getClassLoader()
                ).getResourceAsStream(resPath)
                .readAllBytes()
        );
    }
}
