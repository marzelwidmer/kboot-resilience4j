package ch.keepcalm.demo

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder
import org.springframework.cloud.client.circuitbreaker.Customizer
import org.springframework.context.support.beans
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Year
import java.util.*
import java.util.function.Consumer


@SpringBootApplication
class Resilience4jApplication

fun main(args: Array<String>) {
    runApplication<Resilience4jApplication>(*args) {
        addInitializers(
            beans {
                bean {
                    router {
                        "movies".nest {
                            val service = ref<MovieService>()
                            //http :8080/movies/random
                            GET("/random") {
                                ok().body(service.randomMovie())
                            }
                            //http :8080/movies/ name=="Rocky" -vv
                            queryParam("name", { true }) {
                                ok().body(service.movieByName(name = it.queryParam("name").get()))
                            }
                            //http :8080/movies/c7f399bc-ff4c-4a2f-bddf-d92d53a96df2
                            GET("/{id}") {
                                ok().body(service.movieById(id = it.pathVariable("id")))
                            }
                            //http :8080/movies/random
                            GET("/") {
                                ok().body(service.movies())
                            }
                        }
                    }

                }
                // Client
                bean {
                    WebClient.builder()
                        .baseUrl("http://localhost:8080")
                        .build()
                }
                bean {
                    Customizer<ReactiveResilience4JCircuitBreakerFactory> {
                        Customizer { factory: ReactiveResilience4JCircuitBreakerFactory ->
                            // Default CircuitBreaker Configuration
                            factory.configureDefault { id: String? ->
                                Resilience4JConfigBuilder(id)
                                    .circuitBreakerConfig(CircuitBreakerConfig.custom()
                                        // Configures the size of the sliding window which is used to record the outcome of calls when the CircuitBreaker is closed.
                                        .slidingWindowSize(5)
                                        // Configures the number of permitted calls when the CircuitBreaker is half open.
                                        .permittedNumberOfCallsInHalfOpenState(5)
                                        // Configures the failure rate threshold in percentage
                                        // If the failure rate is equal or greater than the threshold the CircuitBreaker transitions to open and starts short-circuiting calls.
                                        .failureRateThreshold(50.0f)
                                        // Configures the wait duration which specifies how long the CircuitBreaker should stay open, before it switches to half open.
                                        // Default value is 60 seconds.
                                        .waitDurationInOpenState(Duration.ofMillis(30))
                                        // Configures a threshold in percentage. The CircuitBreaker considers a call as slow when the call duration is greater than
                                        // slowCallDurationThreshold(Duration)
                                        //  When the percentage of slow calls is equal or greater the threshold, the CircuitBreaker transitions to open and starts short-circuiting calls.
                                        .slowCallRateThreshold(50.0F)
                                        .build()
                                    ).build()
                            }
                            factory.configure(
                                Consumer {
                                    Resilience4JConfigBuilder("movie-service")
                                        .circuitBreakerConfig(CircuitBreakerConfig.custom()
                                            // Configures the size of the sliding window which is used to record the outcome of calls when the CircuitBreaker is closed.
                                            .slidingWindowSize(5)
                                            // Configures the number of permitted calls when the CircuitBreaker is half open.
                                            .permittedNumberOfCallsInHalfOpenState(1)
                                            // Configures the failure rate threshold in percentage
                                            // If the failure rate is equal or greater than the threshold the CircuitBreaker transitions to open and starts short-circuiting calls.
                                            .failureRateThreshold(100.0f)
                                            // Configures the wait duration which specifies how long the CircuitBreaker should stay open, before it switches to half open.
                                            // Default value is 60 seconds.
                                            .waitDurationInOpenState(Duration.ofMillis(100))
                                            // Configures a threshold in percentage. The CircuitBreaker considers a call as slow when the call duration is greater than
                                            // slowCallDurationThreshold(Duration)
                                            //  When the percentage of slow calls is equal or greater the threshold, the CircuitBreaker transitions to open and starts short-circuiting calls.
                                            .slowCallRateThreshold(200.0F)
                                            .build())
                                }, "movie-service")
                        }
                    }
                    // readySetGo CircuitBreaker
                    bean {
                        ReactiveResilience4JCircuitBreakerFactory().create("movie-service")
                    }
                }
            }
        )
    }
}

data class Movie(val id: String? = UUID.randomUUID().toString(), val name: String, val year: Year, val description: String)

@Service
class MovieService {

    private val movies = listOf(
        Movie(name = "Matrix",
            year = Year.of(1999),
            description = "A computer hacker learns from mysterious rebels about the true nature of his reality and his role in the war against its controllers."),
        Movie(name = "The Godfather",
            year = Year.of(1972),
            description = "The aging patriarch of an organized crime dynasty transfers control of his clandestine empire to his reluctant son."),
        Movie(name = "Casablanca",
            year = Year.of(1942),
            description = "A cynical American expatriate struggles to decide whether or not he should help his former lover and her fugitive husband escape French Morocco."),
        Movie(name = "Rocky",
            year = Year.of(1976),
            description = "A small-time boxer gets a supremely rare chance to fight a heavy-weight champion in a bout in which he strives to go the distance for his self-respect.")
    )

    fun randomMovie() = Mono.just(movies[kotlin.random.Random.nextInt(movies.size)])
    fun movies() = Flux.just(movies)
    fun movieByName(name: String) = Mono.just(movies.first { it.name.toLowerCase() == name.toString().toLowerCase() })
    fun movieById(id: String) = Mono.just(movies.first { it.id == id })

}

//
//@Component
//class Client(private val webClient: WebClient
//    , private val circuitBreaker: ReactiveCircuitBreaker
//) {
//
//    private val log = LoggerFactory.getLogger(javaClass)
//
//
//    @EventListener(classes = [ApplicationReadyEvent::class])
//    fun ready() {
//        for (count in 0..100) {
//            callService(count)
//
//        }
//    }
//
//
//    private fun callService(count: Int) {
//        circuitBreaker.run(
//            webClient
//                .get()
//                .uri("/slow/{name}", "[$count] CallFromEventListener")
//                .retrieve()
//                .bodyToMono(TurtleServiceResponse::class.java)
//                .map { it.message }
//                .onErrorMap { IllegalArgumentException("The original exception was $it.localizedMessage") }
//                .onErrorResume {
//                    when (it) {
//                        is IllegalArgumentException -> Mono.just(it.message.toString())
//                        else -> Mono.just("Ooopss !!! $it.message ")
//                    }
//                }
//        ) {
//            log.warn("-----> !!! $it ")
//            Mono.just(TurtleServiceResponse("Fallback").message)
//        }
//            .subscribe { log.info("--> Client[$count]: $it") }
//    }
//
//}
//
//@Service
//class TurtleService {
//
//    private val log = LoggerFactory.getLogger(javaClass)
//
//    fun readySetGo(name: String?): Mono<TurtleServiceResponse> {
//        name?.map {
//            val seconds = (3..30).random()
//            val msg = "$name Ready, set, go!! this call took $seconds"
//            return Mono.just(TurtleServiceResponse(message = msg))
//                .delayElement(Duration.ofSeconds(seconds.toLong()))
//                .doOnNext { log.info("<-- TurtleService : $it") }
//        }.isNullOrEmpty()
//            .apply {
//                return Mono.error(NullPointerException())
//            }
//    }
//}
//
//data class TurtleServiceResponse(val message: String)
