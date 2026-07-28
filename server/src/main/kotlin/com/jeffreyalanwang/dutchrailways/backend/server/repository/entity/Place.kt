package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField

@Entity
@Table(name = "place")
@Inheritance(strategy = InheritanceType.JOINED)
class Place (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int,

    @field:FullTextField
    @Column(length = 128)
    var name: String,
) {
    @ManyToMany(mappedBy = "contains")
    val locatedIn: MutableSet<Area>? = null
}