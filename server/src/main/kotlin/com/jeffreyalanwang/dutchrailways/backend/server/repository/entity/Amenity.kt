package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField

@Entity
@Table(name = "amenity")
class Amenity (

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id var id: Int = 0,

    @field:KeywordField
    @Column(length = 256)
    var description: String,

)

