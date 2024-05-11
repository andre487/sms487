package life.andre.sms487.auth.errors;

public enum TokenErrorType {
    InternalError,
    InvalidKey,
    InvalidSignature,
    InvalidToken,
    TokenExpired,
    NoExpiration,
    NoIssuedAt,
    NoNotBefore,
    InvalidIssuedAt,
    NoName,
    NoAccessField,
    InvalidAccessField,
}
