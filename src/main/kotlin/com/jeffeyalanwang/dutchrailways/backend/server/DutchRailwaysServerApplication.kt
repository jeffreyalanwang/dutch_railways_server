package com.jeffeyalanwang.dutchrailways.backend.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DutchRailwaysServerApplication

fun main(args: Array<String>) {
    runApplication<DutchRailwaysServerApplication>(*args)
}
