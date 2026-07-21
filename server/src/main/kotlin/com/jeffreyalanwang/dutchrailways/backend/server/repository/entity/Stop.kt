package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import jakarta.persistence.*
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.*

@Table
@Entity
@IdClass(Stop.CompositeId::class)
class Stop(
    @Column("service") @Id var serviceId: Int = -1,
    @Column("arrivetime") @Id var arriveTime: OffsetDateTime? = null,
    @Column("departtime") var departTime: OffsetDateTime? = null, // TODO the database is actually set up to use local times; fix
    @Column("station") var stationId: Int = -1,
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service", insertable = false, updatable = false)
    var service: PassService? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station", insertable = false, updatable = false)
    var station: Station? = null
        protected set

    open class CompositeId : Serializable {

        @Column(name = "service")
        open var serviceId: Int = -1
        open var arriveTime: OffsetDateTime? = null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other::class != this::class) return false

            other as CompositeId

            return serviceId == other.serviceId &&
                    arriveTime == other.arriveTime
        }

        override fun hashCode(): Int = Objects.hash(serviceId, arriveTime)

        companion object {
            private const val serialVersionUID = 0L
        }
    }
}
