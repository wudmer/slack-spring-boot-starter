package com.kreait.slack.broker.store.user

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kreait.slack.api.contract.jackson.SlackEvent
import com.kreait.slack.api.contract.jackson.common.types.Member
import com.kreait.slack.broker.receiver.EventReceiver
import com.kreait.slack.broker.store.team.Team
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders

/**
 * Eventreceiver that updates a user in the registered store when it changes
 */
class UserChangedEventReceiver @Autowired constructor(private val userStore: UserStore) : EventReceiver {

    override fun supportsEvent(slackEvent: SlackEvent): Boolean {
        return slackEvent.event["type"] == "user_change"
    }

    override fun onReceiveEvent(slackEvent: SlackEvent, headers: HttpHeaders, team: Team) {
        val json = jacksonObjectMapper().writeValueAsString(slackEvent.event["user"] as Map<*, *>)
        val user = userOfMember(jacksonObjectMapper().readValue(json, Member::class.java))
        userStore.update(user)
    }
}
