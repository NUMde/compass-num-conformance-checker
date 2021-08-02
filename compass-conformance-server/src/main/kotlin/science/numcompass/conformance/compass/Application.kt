package science.numcompass.conformance.compass

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import science.numcompass.conformance.compass.configuration.CompassConfiguration

@SpringBootApplication
@Import(CompassConfiguration::class)
class Application

fun main(args: Array<String>) {
    @Suppress("SpreadOperator")
    runApplication<Application>(*args)
}
