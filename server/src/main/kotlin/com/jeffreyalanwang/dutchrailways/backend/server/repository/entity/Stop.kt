package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Converter
private class DbTimeZoneConverter: AttributeConverter<Instant?, LocalDateTime?> {
    private val dbTimeZone = ZoneId.of("UTC")

    override fun convertToDatabaseColumn(attribute: Instant?) =
        attribute?.run { atZone(dbTimeZone).toLocalDateTime() }

    override fun convertToEntityAttribute(dbData: LocalDateTime?) =
        dbData?.run { atZone(dbTimeZone).toInstant() }
}

@Table
@Entity
@IdClass(Stop.CompositeId::class)
class Stop(
    @Column("service") @Id var serviceId: Int = -1,

    @Convert(converter = DbTimeZoneConverter::class)
    @Column("arrivetime") @Id var arriveTime: Instant? = null,

    @Convert(converter = DbTimeZoneConverter::class)
    @Column("departtime") var departTime: Instant? = null,

    @Column("station") var stationId: Int = -1,
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service", insertable = false, updatable = false)
    var service: PassService? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station", insertable = false, updatable = false)
    var station: Station? = null

    open class CompositeId : Serializable {

        @Column(name = "service")
        open var serviceId: Int = -1

        @Convert(converter = DbTimeZoneConverter::class)
        open var arriveTime: Instant? = null

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
