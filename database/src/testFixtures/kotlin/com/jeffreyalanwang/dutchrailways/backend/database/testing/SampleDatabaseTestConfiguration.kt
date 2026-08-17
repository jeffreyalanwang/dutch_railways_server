package com.jeffreyalanwang.dutchrailways.backend.database.testing

import libs
import org.slf4j.Logger
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import org.slf4j.LoggerFactory
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.output.OutputFrame.OutputType
import kotlin.time.Duration.Companion.minutes

@TestConfiguration(proxyBeanMethods = false)
object SampleDatabaseTestConfiguration {

    private val testPostgresImage = DockerImageName.parse(libs.versions.docker.postgres.build)

    private const val initScriptName = "dutch_railways_db.sql.gz"

    @get:Bean(destroyMethod = "")
    @get:ServiceConnection
    val dbContainer = PostgreSQLContainer(customPostgres(testPostgresImage))
        .apply {

            withLogConsumer { logger.handleContainerOutput(it) }

            withCopyFileToContainer(
                classpathFile(initScriptName),
                "/docker-entrypoint-initdb.d/$initScriptName",
            ).also {
                // Init script expects this username
                withUsername("postgres")
            }

            // Large amount of data to init might cause timeout
            withReuse(true)
            withStartupTimeoutSeconds(5.minutes.inWholeSeconds.toInt())
            withConnectTimeoutSeconds(3.minutes.inWholeSeconds.toInt())

        }

}

private val logger = LoggerFactory.getLogger(PostgreSQLContainer::class.java)
private fun Logger.handleContainerOutput(output: OutputFrame) = output.run {
    val message = "[$type] $utf8StringWithoutLineEnding"
    when (type) {
        OutputType.STDOUT -> info(message)
        OutputType.STDERR -> error(message)
        else              -> debug(message)
    }
}

private fun customPostgres(customImage: DockerImageName) = customImage.asCompatibleSubstituteFor("postgres")

private fun classpathFile(resourceName: String) = MountableFile.forClasspathResource(resourceName)
