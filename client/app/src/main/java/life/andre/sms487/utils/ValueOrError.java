package life.andre.sms487.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public class ValueOrError<V, E extends Exception> {
    @Nullable
    private final V value;
    @Nullable
    private final E error;

    public ValueOrError(@NonNull V value) {
        this(value, null);
    }

    public ValueOrError(@NonNull E error) {
        this(null, error);
    }

    private ValueOrError(@Nullable V value, @Nullable E error) {
        this.value = value;
        this.error = error;
    }

    public boolean success() {
        return value != null && error == null;
    }

    @Nullable
    public V getValue() {
        return value;
    }

    @NonNull
    public V getValueNonNull() {
        return Objects.requireNonNull(value);
    }

    @Nullable
    public E getError() {
        return error;
    }

    @NonNull
    public E getErrorNonNull() {
        return Objects.requireNonNull(error);
    }

    @NonNull
    public String toString() {
        return "ValueOrError[value=" + value + ", error=" + error + "]";
    }
}
