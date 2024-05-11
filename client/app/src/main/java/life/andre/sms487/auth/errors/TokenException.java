package life.andre.sms487.auth.errors;

public class TokenException extends AuthException {
    public enum TokenErrorType {
        InternalError,
        InvalidKey,
        InvalidToken,
        NoExpiration,
        NoIssuedAt,
        NoNotBefore,
        InvalidIssuedAt,
        NoName,
        NoAccessField,
        InvalidAccessField,
    }

    private final TokenErrorType errorType;

    public TokenException(String message, TokenErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public TokenException(String message, Throwable cause, TokenErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }
}
