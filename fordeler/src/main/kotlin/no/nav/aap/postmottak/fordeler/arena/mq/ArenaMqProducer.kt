package no.nav.aap.postmottak.fordeler.arena.mq

import com.ibm.mq.jakarta.jms.MQConnectionFactory
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import com.ibm.msg.client.jakarta.wmq.compat.base.internal.MQC
import javax.net.ssl.SSLSocketFactory

data class Serviceuser(val username: String, val password: String)

data class MqConfig(
    val mqHostname: String = "localhost",
    val mqPort: Int = 0,
    val mqGatewayName: String = "arena-mq",
    val mqChannelName: String = "arena-mq",
    val serviceuser: Serviceuser
)

fun getMqConnection(config: MqConfig) =
    MQConnectionFactory().apply {
        hostName = config.mqHostname
        port = config.mqPort
        queueManager = config.mqGatewayName
        transportType = WMQConstants.WMQ_CM_CLIENT
        channel = config.mqChannelName
        ccsid = 1208
        sslSocketFactory = SSLSocketFactory.getDefault()
        sslCipherSuite = "*TLS13ORHIGHER"
        setIntProperty(WMQConstants.JMS_IBM_ENCODING, MQC.MQENC_NATIVE)
        setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208)
    }


class ArenaMqProducer {


}