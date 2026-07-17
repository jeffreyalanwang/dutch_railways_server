package com.jeffreyalanwang.dutchrailways.backend.dataSource

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.Objects

@Entity
@Table(name = "stop")
open class Stop {
    @EmbeddedId
    open var id: StopId? = null

    // TODO the database is actually set up to use local times; fix
    @Column(name = "departtime", nullable = false)
    open var departTime: Instant = Instant.now()

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    open var station: Station? = null
        protected set
}

@Embeddable
open class StopId : Serializable {
    @Column(name = "service", nullable = false)
    open var service: Int = 0

    @Column(name = "arrivetime", nullable = false)
    open var arriveTime: Instant = Instant.now()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false

        other as StopId

        return service == other.service &&
                arriveTime == other.arriveTime
    }

    override fun hashCode(): Int = Objects.hash(service, arriveTime)

    companion object {
        private const val serialVersionUID = 0L
    }
}