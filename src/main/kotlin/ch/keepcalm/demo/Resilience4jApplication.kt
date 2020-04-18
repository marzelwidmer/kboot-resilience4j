package ch.keepcalm.demo

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.support.beans
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters.fromValue
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import java.time.Duration

@SpringBootApplication
class Resilience4jApplication

fun main(args: Array<String>) {
    runApplication<Resilience4jApplication>(*args){
        addInitializers(
            beans {
                bean {
                    router {
                        GET("/hello/{name}") {
                            ok().body(fromValue("Hello ${it.pathVariable("name")}"))
                        }
                    }
                }
            }
        )
    }
}

@Service
class TurtleService {
    private val log = LoggerFactory.getLogger(javaClass)

    fun readySetGo(name: String?): Mono<TurtleServiceResponse> {
        name?.map {
            val delayInSeconds = (0..10).random()
            val msg = "$name Ready, set, go!! this call took $delayInSeconds"
            return Mono.just(TurtleServiceResponse(message = msg))
                .delayElement(Duration.ofSeconds(delayInSeconds.toLong()))
                .doOnNext { log.info(it.message) }
        }.isNullOrEmpty()
            .apply {
                return Mono.error(NullPointerException())
            }
    }
}
data class TurtleServiceResponse(val message: String)
