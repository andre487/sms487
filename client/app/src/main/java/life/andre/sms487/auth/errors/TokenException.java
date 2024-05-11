package life.andre.sms487.auth.errors;

public class TokenException extends AuthException {
    private final TokenErrorType errorType;

    public TokenException(String message, TokenErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public TokenException(String message, Throwable cause, TokenErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }

    public TokenErrorType getErrorType() {
        return errorType;
    }
}
