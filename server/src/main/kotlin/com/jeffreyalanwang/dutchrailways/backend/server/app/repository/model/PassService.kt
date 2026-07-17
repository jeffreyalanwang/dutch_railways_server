package com.jeffreyalanwang.dutchrailways.backend.dataSource

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "passservice")
open class PassService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Int = 0

    @Column(name = "name", nullable = false, length = 128)
    open var name: String = ""

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consist", nullable = false)
    open var consist: TrainsetType? =
        null

    @OneToMany(mappedBy = "service")
    open var stops: MutableSet<Stop> =
        mutableSetOf()

}