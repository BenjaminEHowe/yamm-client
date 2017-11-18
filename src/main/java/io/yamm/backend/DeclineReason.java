package io.yamm.backend;

public enum DeclineReason {
    CARD_BLOCKED,               // the card has been blocked, a new card must be ordered
    CARD_INACTIVE,              // the card in inactive, but it can be (re)activated
    FRAUD,                      // suspected fraud
    INSUFFICIENT_CREDIT,        // the card does not have enough credit to perform the requested transaction
    INCORRECT_ADDRESS,          // the billing address supplied was incorrect (online transactions)
    INCORRECT_CVC,              // the CVC supplied was incorrect (online transactions)
    LIMIT_REACHED,              // a spending limit has been reached (e.g. because KYC checks have not been carried out)
    PIN_RETRY_COUNT_EXCEEDED,   // an incorrect PIN has been entered too many times
    UNKNOWN
}
