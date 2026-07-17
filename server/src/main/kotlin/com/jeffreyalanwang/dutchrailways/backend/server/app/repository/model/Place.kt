package com.jeffreyalanwang.dutchrailways.backend.dataSource

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "place")
@Inheritance(strategy = InheritanceType.JOINED)
open class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Int = 0

    @Column(name = "name", nullable = false, length = 128)
    open var name: String = ""

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locatedin")
    open var locatedIn: Area? = null
}