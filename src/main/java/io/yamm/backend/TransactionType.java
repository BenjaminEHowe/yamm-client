package io.yamm.backend;

public enum TransactionType {
    BACS,               // payment by BACS
    CARD,               // card payment (generic)
    CARD_CASH,          // cash withdrawal at an ATM, in branch, or at the Post Office
    CARD_CONTACTLESS,   // contactless card payment
    CARD_MAGSTRIPE,     // magnetic stripe
    CARD_MANUAL,        // manual entry (e.g. from a card imprint)
    CARD_ONLINE,        // online card payment
    CARD_PIN,           // Chip + PIN card payment
    CHAPS,              // payment by CHAPS
    CHEQUE,             // Cheque
    DIRECT_DEBIT,       // Direct Debit
    FASTER_PAYMENT,     // payment by Faster Payments
    FEE,                // a charge / fee from the provider
    INTEREST,           // (credit or debit) interest
    MOBILE,             // mobile payment (generic)
    MOBILE_ANDROID,     // Android pay
    MOBILE_APPLE,       // Apple pay
    MOBILE_FITBIT,      // Fitbit pay
    MOBILE_SAMSUNG,     // Samsung pay
    PAYMENT,            // payment (generic)
    STANDING_ORDER,     // payment by Standing Order
    SWIFT,              // payment by SWIFT
    TRANSFER,           // transfer (usually between accounts held with the same provider)
    UNKNOWN             // something else
}
