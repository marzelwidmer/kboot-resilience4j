# Reactive Spring Boot with Resilience4j CircuitBreaker

## Create Project
Let's create a Sample Application with Kotlin and Reactive Spring Boot with the [spring inializr](https://start.spring.io/) Rest Endpoint. We will take the latest and greates Spring Boot version `2.3.0.M4` and language `kotlin` with the following dependencies:
* actuator
* webflux
* cloud-resilience4j

```bash
http https://start.spring.io/starter.tgz \
    dependencies==actuator,webflux,cloud-resilience4j \
    description=="Demo project Kotlin Spring Boot with Resilience4j" \
    applicationName==Resilience4jApplication \
    name==kboot-resilience4j \
    groupId==ch.keepcalm \
    artifactId==kboot-resilience4j \
    packageName==ch.keepcalm.demo \
    javaVersion==11 \
    language==kotlin \
    bootVersion==2.3.0.M4 \
    baseDir==kboot-resilience4j| tar -xzvf -
```
Add Customer Banner
```bash
http https://raw.githubusercontent.com/marzelwidmer/marzelwidmer.github.io/master/assets/img/2020/spring-initializr/banner.txt \
    > kboot-resilience4j/src/main/resources/banner.txt
```
Configure `spring.applicatin.name`
```bash
echo "spring:
  application:
    name: kboot-resilience4j" | > kboot-resilience4j/src/main/resources/application.yaml
```
Remove `application.properties`
```bash
rm kboot-resilience4j/src/main/resources/application.properties
```

See also my other post [Spring Initializr and HTTPie](https://blog.marcelwidmer.org/spring-initializr/)

## Domain Model
Let's start with the `Movie` domain class with the following properties.
* name
* year
* description

```kotlin
data class Movie(val id: String? = UUID.randomUUID().toString(), val name: String, val year: Year, val description: String)
``` 

## Service 
Now we create the service class `MovieService` who hold some hard coded movies in a list.
The amazing functions:

* Get all Movies
* Get a random list of Movies
* Get a Movie by his name
* Get a Movie by his ID

```kotlin
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
```
## Rest API  
I think now is time to create a REST API `/movies/random` with the [Reactive router Kotlin DSL](https://docs.spring.io/spring-framework/docs/current/kdoc-api/spring-framework/org.springframework.web.reactive.function.server/-router-function-dsl/index.html). 
A easy way to create a `WebFlux.fn` [RouterFunction](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/reactive/function/server/RouterFunctions.html)
Because we have more then one endpoint under `/movies` we use the `"/movies".nest` router function.


We use also our service `MovieService` so we need a [Bean Reference](https://docs.spring.io/spring/docs/current/kdoc-api/spring-framework/org.springframework.context.support/-bean-definition-dsl/-bean-supplier-context/ref.html) to it.

`val service = ref<MovieService>()`

> 💡: Take care of the order in the Route definiton "/" have to be at latest position.

```kotlin
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
            }
        )
    }
}
```

## Test Rest API
No is time to make some calls from the terminal with the `HTTPie` or in a Browser.
First start the application e.g. with `mvn spring-boot:run`.

Then lets call our amazing endpoints with the `HTTPie` or Browser.

### Get all Movies

[http://localhost:8080/movies](http://localhost:8080/movies)

```bash
http :8080/movies/
HTTP/1.1 200 OK
Content-Type: application/json
transfer-encoding: chunked
[
    [
        {
            "description": "A computer hacker learns from mysterious rebels about the true nature of his reality and his role in the war against its controllers.",
            "id": "c7f399bc-ff4c-4a2f-bddf-d92d53a96df2",
            "name": "Matrix",
            "year": "1999"
        },
        {
            "description": "The aging patriarch of an organized crime dynasty transfers control of his clandestine empire to his reluctant son.",
            "id": "0c7a74fc-b735-41a3-a383-72b9dad7d608",
            "name": "The Godfather",
            "year": "1972"
        },
        {
            "description": "A cynical American expatriate struggles to decide whether or not he should help his former lover and her fugitive husband escape French Morocco.",
            "id": "15a9e15d-4a5e-4b8a-a7c5-d34b0d5d0879",
            "name": "Casablanca",
            "year": "1942"
        },
        {
            "description": "A small-time boxer gets a supremely rare chance to fight a heavy-weight champion in a bout in which he strives to go the distance for his self-respect.",
            "id": "f6fb4a62-6d84-434e-8e2c-70e4c1e7ab2c",
            "name": "Rocky",
            "year": "1976"
        }
    ]
]
```

### Get a random list of Movies
   
[http://localhost:8080/movies/random](http://localhost:8080/movies/random)

```bash
 http :8080/movies/random
HTTP/1.1 200 OK
Content-Length: 240
Content-Type: application/json

{
    "description": "A cynical American expatriate struggles to decide whether or not he should help his former lover and her fugitive husband escape French Morocco.",
    "id": "5dd310b8-8d51-4a1e-a20c-790fec00029f",
    "name": "Casablanca",
    "year": "1942"
}
```
 
### Get a Movie by his name
   
[http://localhost:8080/movies/?name=Rocky](http://localhost:8080/movies/?name=Rocky)

```bash
http :8080/movies name=="Rocky" -v
GET /movies?name=Rocky HTTP/1.1
Accept: */*
Accept-Encoding: gzip, deflate
Connection: keep-alive
Host: localhost:8080
User-Agent: HTTPie/2.0.0

HTTP/1.1 200 OK
Content-Length: 242
Content-Type: application/json

{
    "description": "A small-time boxer gets a supremely rare chance to fight a heavy-weight champion in a bout in which he strives to go the distance for his self-respect.",
    "id": "5cae75c6-d18a-495b-a80f-60bdd0763eb1",
    "name": "Rocky",
    "year": "1976"
}
```

### Get a Movie by his ID
   
[http://localhost:8080/movies/5dd310b8-8d51-4a1e-a20c-790fec00029f](http://localhost:8080/movies/5dd310b8-8d51-4a1e-a20c-790fec00029f)

```bash
http :8080/movies/5dd310b8-8d51-4a1e-a20c-790fec00029f
HTTP/1.1 200 OK
Content-Length: 240
Content-Type: application/json

{
    "description": "A cynical American expatriate struggles to decide whether or not he should help his former lover and her fugitive husband escape French Morocco.",
    "id": "5dd310b8-8d51-4a1e-a20c-790fec00029f",
    "name": "Casablanca",
    "year": "1942"
}
```

### Search for a not existing Movie
Now let's also search for `Creed` is also a great movie but this one is not yet in our 'MovieService' included
we will get the following exception.
```bash
http :8080/movies/creed
HTTP/1.1 500 Internal Server Error
Content-Length: 206
Content-Type: application/json

{
    "error": "Internal Server Error",
    "message": "Collection contains no element matching the predicate.",
    "path": "/movies/creed",
    "requestId": "4eac7833-10",
    "status": 500,
    "timestamp": "2020-04-19T17:04:12.054+00:00"
}
```


















`http :8080/hello/world`

```bash
HTTP/1.1 200 OK
Content-Length: 11
Content-Type: text/plain;charset=UTF-8

Hello world
```

Let's call the `TurtleService` now from a Router API `/slow/{name}` for this we can get a [Bean Reference](https://docs.spring.io/spring/docs/current/kdoc-api/spring-framework/org.springframework.context.support/-bean-definition-dsl/-bean-supplier-context/ref.html)
`val service = ref<TurtleService>()` an call our `readySetGo` function.
```kotlin
fun main(args: Array<String>) {
    runApplication<Resilience4jApplication>(*args){
        addInitializers(
            beans {
                bean {
                    router {
                        GET("/slow/{name}") {
                            val service = ref<TurtleService>() // Bean Reference
                            ok().body(service.readySetGo(name = it.pathVariable("name")))
                        }
                    }
                }
            }
        )
    }
}
```

Let's test the new API first with a loop and call the service `50` times. 
```bash
for i in {1..50}; do http "http://localhost:8080/slow/Service" ; done

HTTP/1.1 200 OK
Content-Length: 55
Content-Type: application/json

{
    "message": "Service Ready, set, go!! this call took 2"
}

HTTP/1.1 200 OK
Content-Length: 55
Content-Type: application/json

{
    "message": "Service Ready, set, go!! this call took 7"
}

HTTP/1.1 200 OK
Content-Length: 55
Content-Type: application/json

{
    "message": "Service Ready, set, go!! this call took 9"
}
```

Let's go one step further and refactor again our code. 
Create first a new `Bean` for the `WebClient` with this we will call our Service on Application Start.
This will simulate our `Httpie` call from before.

```kotlin
// Client
bean {
    WebClient.builder()
        .baseUrl("http://localhost:8080")
        .build()
}
```  

Then create a `Client` Spring `Component` class with the name `Client` where we inject our `WebClient`.
We also will implement a function `ready` with a loop `for (count in 1..50)` like before on application start for this we use the Spring `EventListener`
and the `@EventListener(classes = [ApplicationReadyEvent::class])`  annotation.

```kotlin
@Component
class Client(private val webClient: WebClient) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(classes = [ApplicationReadyEvent::class])
    fun ready() {

        for (count in 0..50) {
            webClient
                .get()
                .uri("/slow/{name}", "[$count]CallFromEventListener")
                .retrieve()
                .bodyToMono(TurtleServiceResponse::class.java)
                .map { it.message }
                .subscribe {
                    log.info("--> Client[$count] :  $it")
                }
        }
    }
}

```

Now when we start our application again `mvn spring-boot:run` you will see in the logfile something like this hopefully.

```bash
2020-04-19 Sun 09:45:43.793 TurtleService - <-- TurtleService : TurtleServiceResponse(message=[25]CallFromEventListener Ready, set, go!! this call took 3)
2020-04-19 Sun 09:45:43.795 Client        - --> Client[25] :  [25]CallFromEventListener Ready, set, go!! this call took 3
```

> 💡 **Logger Configuration**: 
    logging.pattern.console: "%clr(%d{yyyy-MM-dd E HH:mm:ss.SSS}){blue} %clr(%-40.40logger{0}){magenta} - %clr(%m){green}%n" 
    

Now we configure our `WebClient` a timeout of `5 seconds` with `.timeout(Duration.ofSeconds(5.toLong()))` and start the application 
again `mvn spring-boot:run` to see a `java.util.concurrent.TimeoutException` after a while.  
 
```bash
2020-04-19 Sun 09:59:47.804 TurtleService    - <-- TurtleService : TurtleServiceResponse(message=[34]CallFromEventListener Ready, set, go!! this call took 4)
2020-04-19 Sun 09:59:47.805 Client           - --> Client[34] :  [34]CallFromEventListener Ready, set, go!! this call took 4
2020-04-19 Sun 09:59:48.522 Schedulers       - Scheduler worker in group main failed with an uncaught exception
reactor.core.Exceptions$ErrorCallbackNotImplemented: java.util.concurrent.TimeoutException: Did not observe any item or terminal signal within 5000ms in 'map' (and no fallback has been configured)
Caused by: java.util.concurrent.TimeoutException: Did not observe any item or terminal signal within 5000ms in 'map' (and no fallback has been configured)
	at reactor.core.publisher.FluxTimeout$TimeoutMainSubscriber.handleTimeout(FluxTimeout.java:289) ~[reactor-core-3.3.4.RELEASE.jar:3.3.4.RELEASE]
	at reactor.core.publisher.FluxTimeout$TimeoutMainSubscriber.doTimeout(FluxTimeout.java:274) ~[reactor-core-3.3.4.RELEASE.jar:3.3.4.RELEASE]
	at reactor.core.publisher.FluxTimeout$TimeoutTimeoutSubscriber.onNext(FluxTimeout.java:396) ~[reactor-core-3.3.4.RELEASE.jar:3.3.4.RELEASE]
```

😎 Cool stuff 😎 let's implement the `CircuitBreaker` with `Resilinece4j`. 

For this we create a `ReactiveCircuitBreaker` Bean from `ReactiveResilience4JCircuitBreakerFactory` with a name `readySetGo`.

```kotlin
bean {
    ReactiveResilience4JCircuitBreakerFactory()
        .create("readySetGo")
}
```

That Bean will be also injected in our `Client` class `private val circuitBreaker: ReactiveCircuitBreaker`
```kotlin
class Client(private val webClient: WebClient, private val circuitBreaker: ReactiveCircuitBreaker)
```

Let's define now our Pipeline with the `CircuitBreaker` `run` function and implement a dummy implementation with a log statment when the `CircuitBreaker` is open.
`Mono.just("Ooopss CircuitBreaker  is [OPEN ]!!! $it.message ")`

```kotlin
circuitBreaker.run(
            webClient
                .get()
                .uri("/slow/{name}", "[$count]CallFromEventListener")
                .retrieve()
                .bodyToMono(TurtleServiceResponse::class.java)
                .map { it.message }
                .timeout(Duration.ofSeconds(5.toLong()))

        ) { // it: Throwable
            Mono.just("Ooopss CircuitBreaker !!! $it ")
        }
            .subscribe { log.info("--> Client[$count] :  $it") }
```

so far we haven't got a better result.  
```bash
2020-04-19 Sun 10:30:35.105 Client - --> Client[4] :  [4]CallFromEventListener Ready, set, go!! this call took 0
2020-04-19 Sun 10:30:35.733 Client - --> Client[0] :  CircuitBreaker is [OPEN ]!!! java.util.concurrent.TimeoutException: Did not observe any item or terminal signal within 1000ms in 'circuitBreaker' (and no fallback has been configured)
```
 




The example source code can be found here [GitHub](https://github.com/marzelwidmer/kboot-resilience4j)



> **_References:_**  
>[Resilience4j docs](https://resilience4j.readme.io/docs)
 

 