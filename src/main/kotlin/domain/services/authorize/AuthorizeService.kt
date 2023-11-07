package domain.services.authorize

import domain.payment.sideeffectevents.EventPublisher
import domain.payment.state.*
import domain.repositories.PaymentRepository
import domain.repositories.PaymentRepositoryLegacy
import domain.services.featureflag.FeatureFlag
import domain.services.featureflag.FeatureFlag.Feature.DECOUPLED_AUTH
import domain.services.fraud.RiskAssessmentService
import domain.services.gateway.AuthorizationGateway
import domain.services.routing.RoutingService

class AuthorizeService
(
    private val riskService: RiskAssessmentService,
    private val routingService: RoutingService,
    private val authorizeService: AuthorizationGateway,
    private val paymentRepository: PaymentRepository,
    private val paymentRepositoryLegacy: PaymentRepositoryLegacy,
    private val eventPublisher: EventPublisher,
    private val featureFlag: FeatureFlag
)
{
    // DECOUPLED:
    //------------------------------------------------------------------------------------------------------------------

    fun authorize(payment: Payment): Payment
    {
        return saveAndSendEvents(payment)
            .letAndSaveIf { it: ReadyForRisk -> it.addFraudAnalysisResult(riskService.assessRisk(it)) }
            .letIf { it: ReadyForRoutingInitial -> tryToAuthorize(it) }
            .letAndSaveIf { it: ReadyToEndAuthorization -> it.checkIfPaymentCaptured() }
    }

    private fun tryToAuthorize(payment: ReadyForRouting): Payment
    {
        return payment
            .letAndSaveIf { it: ReadyForRouting -> it.addRoutingResult(routingService.routeForPayment(it)) }
            .letAndSaveIf { it: ReadyToDecideAuthMethod -> it.decideAuthMethod(featureFlag.isFeatureEnabledFor(DECOUPLED_AUTH)) }
            .letAndSaveIf { it: ReadyForAuth -> it.addAuthenticationResponse(authorizeService) }
            .letAndSaveIf { it: ReadyToVerifyAuthentication -> it.verifyECI()  }
            .letAndSaveIf { it: ReadyForAuthorization -> it.addAuthorizeResponse(authorizeService.authorize(it)) }
            .letAndSaveIf { it: RejectedByGateway -> it.prepareForRetry()  }
            .letIf { it: ReadyForRoutingRetry -> tryToAuthorize(it) }
    }

    fun confirm(payment: Payment, confirmParams: Map<String, Any>): Payment
    {
        return payment
            .letAndSaveIf { it: ReadyToReturnFromAuthenticationAndAuthorization -> it.addConfirmParameters(confirmParams)  }
            .letAndSaveIf { it: ReadyToContinuaAuthenticationAndAuthorization -> it.addAuthenticateConfirmResponse(authorizeService.continueAuthenticateAndAuthorize(it) ) }
            .letAndSaveIf { it: ReadyToReturnFromAuthentication -> it.addConfirmParameters(confirmParams) }
            .letAndSaveIf { it: ReadyToContinueAuthentication -> it.addAuthenticateConfirmResponse(authorizeService.continueAuthenticate(it) ) }
            .letAndSaveIf { it: ReadyToVerifyAuthentication -> it.verifyECI()  }
            .letAndSaveIf { it: ReadyForAuthorization -> it.addAuthorizeResponse(authorizeService.authorize(it)) }
            .letAndSaveIf { it: RejectedByGateway -> it.prepareForRetry()  }
            .letIf { it: ReadyForRoutingRetry -> tryToAuthorize(it) }
            .letAndSaveIf { it: ReadyToEndAuthorization -> it.checkIfPaymentCaptured() }
    }

    // PERSISTENCE:
    //------------------------------------------------------------------------------------------------------------------

    private fun saveAndSendEvents(payment: Payment): Payment
    {
        payment.sideEffectEvents.forEach { eventPublisher.publish(it) }
        return paymentRepository.save(payment)
            .flushSideEffectEvents()
            .flushPaymentEvents()
            .also { paymentRepositoryLegacy.save(it) }
    }

    // HELPER:
    //------------------------------------------------------------------------------------------------------------------

    private inline fun <reified T, R>R.letIf(function: (T) -> R): R =

        if (this is T) function.invoke(this as T) else this

    private inline fun <reified T> Payment.letAndSaveIf(function: (T) -> Payment): Payment =

        if (this is T) saveAndSendEvents(function.invoke(this)) else this
}
