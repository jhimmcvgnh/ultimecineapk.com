package com.nuvio.app.features.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 34], qualifiers = "w600dp-h960dp")
class AuthSmokeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun smokePaperCard_rendersWithoutCrashAcrossApiLevels() {
        composeRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.size(600.dp, 680.dp)) {
                    SmokePaperCard(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                        ) {
                            Text(
                                text = "Cine Ultimate Smoke Hero",
                                modifier = Modifier.align(Alignment.BottomStart),
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("Cine Ultimate Smoke Hero").assertIsDisplayed()
    }

    @Test
    fun paywallPreCheckoutSection_preservesPaymentAndProfileLoginActions() {
        var checkoutClicked = false
        var trialClicked = false
        var loginClicked = false

        composeRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.size(440.dp, 860.dp)) {
                    AppPaywallPreCheckoutSection(
                        metrics = LargeAuthFormMetrics,
                        scale = 1f,
                        primaryHeight = 52.dp,
                        priceLabel = "R$ 29,00",
                        isWaitingPayment = false,
                        confirmedPaidEmail = null,
                        manualCheckEmail = "",
                        showManualEmailFallback = false,
                        isConfirmingPayment = false,
                        authError = null,
                        onGoToCheckout = { checkoutClicked = true },
                        onStartFreeTrial = { trialClicked = true },
                        onManualCheckEmailChange = {},
                        onProceedPaid = {},
                        onOpenExistingAccountLogin = { loginClicked = true },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Libere o Acesso ao Cine Ultimate").assertIsDisplayed()
        composeRule.onNodeWithText("Pagamento Único de R$ 29,00 (Sem mensalidade)").assertIsDisplayed()
        composeRule.onNodeWithText("⚡ Pagar R$ 29,00 no Site e Liberar").assertIsDisplayed().performClick()
        assertTrue(checkoutClicked)

        composeRule.onNodeWithText("🎁 Teste Grátis de 20 Dias (Criar Conta)").assertIsDisplayed().performClick()
        assertTrue(trialClicked)

        composeRule.onNodeWithText("Já possui conta e senha cadastradas? Fazer Login").assertIsDisplayed().performClick()
        assertTrue(loginClicked)
    }

    @Test
    fun paywallPreCheckoutSection_locksFreeTrialWhen20DaysExpiredAndRequiresSameGmail() {
        var checkoutClicked = false

        composeRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.size(440.dp, 860.dp)) {
                    AppPaywallPreCheckoutSection(
                        metrics = LargeAuthFormMetrics,
                        scale = 1f,
                        primaryHeight = 52.dp,
                        priceLabel = "R$ 29,00",
                        isWaitingPayment = false,
                        confirmedPaidEmail = null,
                        manualCheckEmail = "teste20dias@gmail.com",
                        showManualEmailFallback = false,
                        isConfirmingPayment = false,
                        lockedTrialEmail = "teste20dias@gmail.com",
                        isTrialExpired = true,
                        authError = null,
                        onGoToCheckout = { checkoutClicked = true },
                        onManualCheckEmailChange = {},
                        onProceedPaid = {},
                        onOpenExistingAccountLogin = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("⏳ TESTE GRÁTIS DE 20 DIAS ENCERRADO").assertIsDisplayed()
        composeRule.onNodeWithText("⚡ Pagar R$ 29,00 no Site e Liberar").assertIsDisplayed().performClick()
        assertTrue(checkoutClicked)
    }

    @Test
    fun paywallPreCheckoutSection_displaysManualVerificationAndProceedButtonWhenWaiting() {
        var proceedClicked = false
        var emailValue = ""

        composeRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.size(440.dp, 820.dp)) {
                    AppPaywallPreCheckoutSection(
                        metrics = LargeAuthFormMetrics,
                        scale = 1f,
                        primaryHeight = 52.dp,
                        priceLabel = "R$ 29,00",
                        isWaitingPayment = true,
                        confirmedPaidEmail = null,
                        manualCheckEmail = emailValue,
                        showManualEmailFallback = true,
                        isConfirmingPayment = false,
                        authError = null,
                        onGoToCheckout = {},
                        onManualCheckEmailChange = { emailValue = it },
                        onProceedPaid = { proceedClicked = true },
                        onOpenExistingAccountLogin = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Gmail usado no PIX do site").assertIsDisplayed()
        composeRule.onNodeWithText("✓ Já fiz o pagamento • Prosseguir →").assertIsDisplayed().performClick()
        assertTrue(proceedClicked)
        assertEquals("", emailValue)
    }

    @Test
    fun animatedSignInCard_rendersLoginAndVerifiedSignUpAndTriggersCallbacks() {
        var submitCount = 0
        var toggleCount = 0
        var backCount = 0

        composeRule.setContent {
            MaterialTheme {
                AnimatedLoginAtmosphere(modifier = Modifier.size(440.dp, 860.dp)) {
                    AnimatedSignInCard(
                        priceLabel = "R$ 29,00",
                        verifiedPaidEmail = "cliente@gmail.com",
                        isSignUp = true,
                        email = "cliente@gmail.com",
                        password = "secret123",
                        passwordVisible = false,
                        isLoading = false,
                        authError = null,
                        metrics = LargeAuthFormMetrics,
                        scale = 1f,
                        headingStyle = MaterialTheme.typography.headlineSmall,
                        subtitleStyle = MaterialTheme.typography.bodyMedium,
                        onEmailChange = {},
                        onPasswordChange = {},
                        onPasswordVisibilityToggle = {},
                        onSubmit = { submitCount++ },
                        onToggleAuthMode = { toggleCount++ },
                        onReturnToPaywall = { backCount++ },
                        onEmailBoundsChange = {},
                        onPasswordBoundsChange = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Criar Conta Principal (Host)").assertIsDisplayed().performClick()
        assertEquals(1, submitCount)
        composeRule.onNodeWithText("← Voltar para a Tela de Pagamento PIX (R$ 29,00)").assertIsDisplayed().performClick()
        assertEquals(1, backCount)
        assertEquals(0, toggleCount)
    }

    @Test
    fun animatedSignInCard_renders20DayFreeTrialSignUpMode() {
        var trialSubmitted = 0

        composeRule.setContent {
            MaterialTheme {
                AnimatedLoginAtmosphere(modifier = Modifier.size(440.dp, 880.dp)) {
                    AnimatedSignInCard(
                        priceLabel = "R$ 29,00",
                        verifiedPaidEmail = null,
                        isSignUp = true,
                        isTrialSignUp = true,
                        email = "novo@gmail.com",
                        password = "secret123",
                        passwordVisible = false,
                        isLoading = false,
                        authError = null,
                        metrics = LargeAuthFormMetrics,
                        scale = 1f,
                        headingStyle = MaterialTheme.typography.headlineSmall,
                        subtitleStyle = MaterialTheme.typography.bodyMedium,
                        onEmailChange = {},
                        onPasswordChange = {},
                        onPasswordVisibilityToggle = {},
                        onSubmit = { trialSubmitted++ },
                        onToggleAuthMode = {},
                        onReturnToPaywall = {},
                        onEmailBoundsChange = {},
                        onPasswordBoundsChange = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Teste Grátis • 20 Dias").assertIsDisplayed()
        composeRule.onNodeWithText("Ativar Teste Grátis de 20 Dias").assertIsDisplayed().performClick()
        assertEquals(1, trialSubmitted)
    }
}
