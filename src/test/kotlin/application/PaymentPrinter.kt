package application

import domain.payment.paymentevents.PaymentEvent
import domain.payment.state.*

class PaymentPrinter
{
    fun printPaymentStates(events: List<PaymentEvent>)
    {
        var path = "\nPATH:\n\n"

        events.foldIndexed(ReadyForPaymentRequest() as Payment) { index, previousState, event ->

            val currentState = previousState.apply(event, false)
            val isLastEvent = isLastEvent(index, events)

            path = "$path${previousState::class.java.simpleName} -> ${event::class.java.simpleName} -> ${currentState::class.java.simpleName}\n"
            printState(currentState, isLastEvent)

            currentState
        }

        println(path)
    }

    private fun isLastEvent(index: Int, events: List<PaymentEvent>) = index == events.size - 1

    private fun printState(payment: Payment, isLastEvent: Boolean)
    {
        when (payment)
        {
            is ReadyForPaymentRequest -> Unit
            is ReadyForRisk -> Unit
            is ReadyForRoutingInitial -> Unit
            is ReadyToContinueAuthentication -> Unit
            is ReadyToContinueAuthenticationAndAuthorization -> Unit
            is ReadyForRoutingRetry -> Unit
            is ReadyToEndAuthorization -> Unit
            is ReadyToDecideAuthMethod -> Unit
            is ReadyToVerifyAuthentication -> Unit

            is ReadyToInitiateAuthenticationAndAuthorization -> if (isLastEvent) println(payment)
            is ReadyToInitiateAuthentication -> if (isLastEvent) println(payment)
            is ReadyForAuthorization -> if (isLastEvent) println(payment)

            // PENDING STATES:
            //----------------------------------------------------------------------------------------------------------

            is ReadyToReturnFromAuthentication -> if (isLastEvent) println(payment)
            is ReadyToReturnFromAuthenticationAndAuthorization ->if (isLastEvent) println(payment)

            // FINAL STATES:
            //----------------------------------------------------------------------------------------------------------

            is RejectedByGatewayAndNotRetriable -> Unit
            is RejectedByRoutingSameAccount -> Unit

            is Authorized -> println(payment)
            is Captured -> println(payment)
            is Failed -> println(payment)
            is RejectedByAuthorization -> println(payment)
            is RejectedByAuthentication -> println(payment)
            is RejectedByECIVerification -> println(payment)
            is RejectedByRisk -> println(payment)
            is RejectedByRouting -> println(payment)
        }
    }
}
