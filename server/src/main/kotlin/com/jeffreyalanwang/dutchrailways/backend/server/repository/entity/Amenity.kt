package com.jeffreyalanwang.dutchrailways.backend.server.repository.entity

import jakarta.persistence.*

@Entity
@Table(name = "amenity")
class Amenity (

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id var id: Int = 0,

    @Column(length = 256)
    var description: String,

)