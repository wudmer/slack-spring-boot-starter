package com.kreait.slack.broker.store

import com.kreait.slack.broker.extensions.sample
import com.kreait.slack.broker.store.user.InMemoryUserStore
import com.kreait.slack.broker.store.user.User
import com.kreait.slack.broker.store.user.UserNotFoundException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("InMemoryUserStore")
class InMemoryUserStoreTests {


    @DisplayName("Test Add and Remove")
    @Test
    fun addAndRemoveUser() {

        val inMemoryUserStore = InMemoryUserStore()
        inMemoryUserStore.put(User.sample().copy(id = "TestUserId"))

        assertEquals(User.sample().copy(id = "TestUserId"), inMemoryUserStore.findById("TestUserId"))

        inMemoryUserStore.removeById("TestUserId")
        assertThrows(UserNotFoundException::class.java) { inMemoryUserStore.findById("TestUserId") }
    }


    @DisplayName("Test Add and Remove")
    @Test
    fun findUserByTeam() {
        val inMemoryUserStore = InMemoryUserStore()
        inMemoryUserStore.put(User.sample().copy(id = "TestUserId1", teamId = "TestTeamId1"))
        inMemoryUserStore.put(User.sample().copy(id = "TestUserId2", teamId = "TestTeamId2"))

        assertEquals(1, inMemoryUserStore.findByTeam("TestTeamId1").size)
    }

    @DisplayName("Remove Non Existent")
    @Test
    fun removeNonExistent() {
        val inMemoryUserStore = InMemoryUserStore()
        assertDoesNotThrow { inMemoryUserStore.removeById("NonExistentUserId") }

    }
}
