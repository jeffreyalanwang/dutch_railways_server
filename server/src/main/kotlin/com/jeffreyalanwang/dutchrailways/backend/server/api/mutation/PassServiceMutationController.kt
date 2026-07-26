package com.jeffreyalanwang.dutchrailways.backend.server.api

import com.jeffreyalanwang.dutchrailways.backend.server.dto.MutationPassService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.PassServiceRepository
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.PassService
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Stop
import com.jeffreyalanwang.dutchrailways.backend.server.repository.entity.Stop.Comparators.byArriveTime
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Controller
class PassServiceMutationController(
    val passServiceRepository: PassServiceRepository,
) {
    
    @Transactional
    @MutationMapping
    fun createPassService(@Argument details: MutationPassService): PassService? = with(passServiceRepository) {
        PassService().run {
            applyFrom(details)
            stops.checkTimetable()
            save(this)
        }
    }

    // TODO modify database so that amenities are applied on the Trainset
    @Transactional
    @MutationMapping
    fun updatePassService(@Argument id: Int, @Argument details: MutationPassService): PassService? = with(passServiceRepository) {
        findById(id).getOrNull()
            ?.run {
                applyFrom(details)
                stops.checkTimetable()
                save(this)
            }
    }

    @Transactional
    @MutationMapping
    fun deletePassService(@Argument id: Int): Int? = with(passServiceRepository) {
        if (existsById(id)) id.also { deleteById(id) }
        else null
    }

}

context(repository: PassServiceRepository)
private fun PassService.applyFrom(obj: MutationPassService) = apply {
    name = obj.name

    consist = repository.getTrainsetEntity(obj.trainset)
    consist!!.amenities
        .apply { clear() }
        .addAll(
            repository.getAmenityEntity(obj.amenityEnums)
        )

    stops
        .apply { clear() }
        .addAll(
            obj.stops.map { inputStop ->
                Stop(
                    serviceId = id,
                    stationId = inputStop.station,
                    arriveTime = inputStop.arriveTime?.toInstant(),
                    departTime = inputStop.departTime?.toInstant(),
                )
            }
        )
}

private fun Collection<Stop>.checkTimetable() = sortedWith(byArriveTime).run {
    check( isNotEmpty() )

    // We do not need to check [serviceId] because JPA will set this value
    // to the associated PassService entity on save.

    check( allDistinct { it.stationId } )

    check( first().arriveTime == null )
    check( last().departTime == null)

    zipWithNext { (_, depart), (arrive, _) ->
        check(depart != null)
        check(arrive != null)
        check(depart < arrive)
    }

    drop(1).dropLast(1).forEach { (arrive, depart) ->
        check(arrive!! < depart!!)
    }
}

private inline fun <T, K> Collection<T>.allDistinct(selector: (T) -> K): Boolean {
    val set = HashSet<K>(size)
    return all { set.add( selector(it) ) }
}
