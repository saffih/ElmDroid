package saffih.elmdroid.gps

import android.content.Context
import saffih.elmdroid.service.client.ElmMessengerServiceClient
abstract class GpsElmServiceClient(me: Context) :
        ElmMessengerServiceClient<MsgApi>(me, javaClassName = GpsService::class.java,
                toApi = { it.toApi() },
                toMessage = { it.toMessage() })