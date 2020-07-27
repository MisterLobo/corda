package net.corda.node.services.network

import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.NetworkParametersStorage
import net.corda.core.node.NotaryInfo
import net.corda.node.internal.NetworkParametersReader
import net.corda.testing.common.internal.addNotary
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito

import org.mockito.MockitoAnnotations
import org.mockito.Spy
import java.nio.file.Paths
import java.security.cert.X509Certificate

class NetworkParametersUpdaterTest {

    @Spy
    val notary: Party = TestIdentity.fresh("test notary").party
    @Spy
    val networkParameters = testNetworkParameters()
    @Spy
    val baseDirectory= Paths.get("./")
    @Mock
    val trustRoot = Mockito.mock(X509Certificate::class.java)
    @Mock
    val parametersHash = Mockito.mock(SecureHash::class.java)
    @Mock
    val networkParametersReader = Mockito.mock(NetworkParametersReader::class.java)
    @Mock
    val networkParametersStorage = Mockito.mock(NetworkParametersStorage::class.java)

    val newnetParamsWithNewNotary = networkParameters.addNotary(notary)

    @InjectMocks
    lateinit var networkParametersUpdater: NetworkParametersUpdater


    @Before
    fun init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(timeout=300_000)
    fun `can not hotload if notary changes but no listener function exists`() {

        Assert.assertFalse(networkParametersUpdater.canHotload(newnetParamsWithNewNotary))
    }

    @Test(timeout=300_000)
    fun `can hotload if notary changes`() {

        networkParametersUpdater.addNotaryUpdateListener(object: NotaryListUpdateListener {
            override fun onNewNotaryList(notaries: List<NotaryInfo>) {
            }
        })
        Assert.assertTrue(networkParametersUpdater.canHotload(newnetParamsWithNewNotary))
    }

    @Test(timeout=300_000)
    fun `can hotload if only always hotloadable properties change`() {

        val newParametersWithAlwaysHotloadableProperties = networkParameters.copy(epoch = networkParameters.epoch +1, modifiedTime = networkParameters.modifiedTime.plusSeconds(60))
        Assert.assertTrue(networkParametersUpdater.canHotload(newParametersWithAlwaysHotloadableProperties))
    }

    @Test(timeout=300_000)
    fun `can not hotload if any other property changes`() {

        val parametersWithNewMaxMessageSize = networkParameters.copy(maxMessageSize = networkParameters.maxMessageSize +1)
        val parametersWithNewTransactionSize = networkParameters.copy(maxTransactionSize = networkParameters.maxTransactionSize +1)
        val parametersWithNewminimumPlatformVersion = networkParameters.copy(maxTransactionSize = networkParameters.minimumPlatformVersion +1)

        Assert.assertFalse(networkParametersUpdater.canHotload(parametersWithNewMaxMessageSize))
        Assert.assertFalse(networkParametersUpdater.canHotload(parametersWithNewTransactionSize))
        Assert.assertFalse(networkParametersUpdater.canHotload(parametersWithNewminimumPlatformVersion))

    }
}

