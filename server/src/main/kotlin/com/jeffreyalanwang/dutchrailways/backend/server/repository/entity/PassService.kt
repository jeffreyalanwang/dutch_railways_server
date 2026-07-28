package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded

@Indexed
@Entity
@Table(name = "passservice")
class PassService(

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id var id: Int = -1,

    @field:FullTextField
    @Column(length = 128)
    var name: String = "",

) {

    @IndexedEmbedded(includeDepth = 2)
    @ManyToOne(optional = false)
    @JoinColumn(name = "consist", nullable = false)
    var consist: TrainsetType? = null

    @OneToMany(mappedBy = "service", cascade = [CascadeType.ALL])
    val stops: MutableSet<Stop> = mutableSetOf()

}