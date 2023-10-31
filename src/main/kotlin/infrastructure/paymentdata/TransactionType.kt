package infrastructure.paymentdata

enum class TransactionType
{
    AUTHORIZE,
    AUTHORIZE_AND_SETTLE,
    CANCEL,
    CAPTURE,
    REFUND;
}
