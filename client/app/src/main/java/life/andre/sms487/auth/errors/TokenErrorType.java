package life.andre.sms487.auth.errors;

public enum TokenErrorType {
    InternalError,
    InvalidKey,
    InvalidSignature,
    InvalidToken,
    NoExpiration,
    NoIssuedAt,
    NoNotBefore,
    InvalidIssuedAt,
    NoName,
    NoAccessField,
    InvalidAccessField,
}
