package com.jeffreyalanwang.dutchrailways.backend.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

fun main(args: Array<String>) {
    runApplication<com.jeffreyalanwang.dutchrailways.backend.server.DutchRailwaysServerApplication>(*args)
}

@SpringBootApplication
class DutchRailwaysServerApplication
