package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField

@Table
@Entity
class TrainsetType (
    @field:KeywordField
    @Column(length = 64)
    @Id val name: String,

    @field:IndexedEmbedded(includeDepth = 1)
    @ManyToMany
    @JoinTable(
        "trainsetamenities",
        joinColumns = [JoinColumn("trainsettype")],
        inverseJoinColumns = [JoinColumn("amenity")],
    )
    var amenities: MutableSet<Amenity>,
)