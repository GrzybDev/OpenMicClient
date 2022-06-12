package pl.grzybdev.openmic.client.network.messages.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Auth_CodeVerify")
class AuthCodeVerify(
    val authCode: Int
) : ClientPacket()

@Serializable
@SerialName("Auth_ClientSide")
class AuthClientSide() : ClientPacket()
