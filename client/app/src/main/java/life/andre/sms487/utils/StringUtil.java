package life.andre.sms487.utils;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StringUtil {
    @NonNull
    public static String joinUnique(@NonNull List<String> parts, @NonNull String joiner) {
        StringBuilder msg = new StringBuilder();
        Set<String> existing = new HashSet<>();

        int lastIdx = parts.size() - 1;
        for (int i = 0; i < parts.size(); ++i) {
            String part = parts.get(i);
            if (existing.contains(part)) {
                continue;
            }
            existing.add(part);

            msg.append(part);
            if (i < lastIdx) {
                msg.append(joiner);
            }
        }

        return msg.toString();
    }
}
