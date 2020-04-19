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

## Service 
Let's create a slow service `TurtleService` with a response class `TurtleServiceResponse`.
The Publisher have some nice functions to simulate a slow service. `.delayElement(Duration.ofSeconds(delayInSeconds.toLong()))`
We also will log the message with the `.doOnNext { log.info(it.message) }` function on server side.

```kotlin
@Service
class TurtleService {
    private val log = LoggerFactory.getLogger(javaClass)

    fun readySetGo(name: String?): Mono<TurtleServiceResponse> {
        name?.map {
            val delayInSeconds = (0..10).random()
            val msg = "$name Ready, set, go!! this call took $delayInSeconds"
            return Mono.just(TurtleServiceResponse(message = msg))
                .delayElement(Duration.ofSeconds(delayInSeconds.toLong()))
                .doOnNext { log.info("$it") }
        }.isNullOrEmpty()
            .apply {
                return Mono.empty()
            }
    }
}
data class TurtleServiceResponse(val message: String)
```

## API
Let's create a REST API `/slow/{name}` with the [Reactive router Kotlin DSL](https://docs.spring.io/spring-framework/docs/current/kdoc-api/spring-framework/org.springframework.web.reactive.function.server/-router-function-dsl/index.html). 
A easy way to create a `WebFlux.fn` [RouterFunction](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/reactive/function/server/RouterFunctions.html)

We start with a `Hello World` Endpoint `/hello/{name}` and will refactor it later. 
```kotlin
fun main(args: Array<String>) {
    runApplication<Resilience4jApplication>(*args){
        addInitializers(
            beans { // this: BeanDefinitionDsl
                bean { // this: BeanDefinitionDsl.BeanSupplierContext
                    router { // this: RouterFunctionDsl
                        GET("/hello/{name}") { // it: ServerRequest
                            ok().body(fromValue("Hello ${it.pathVariable("name")}"))
                        }
                    }
                }
            }
        )
    }
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

        for (count in 1..50) {
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
020-04-19 07:54:49.756  INFO 6512 --- [parallel-11] ch.keepcalm.demo.TurtleService: TurtleServiceResponse(message=[5]CallFromEventListener Ready, set, go!! this call took 2)
2020-04-19 07:54:49.756  INFO 6512 --- [parallel-1] ch.keepcalm.demo.TurtleService: TurtleServiceResponse(message=[8]CallFromEventListener Ready, set, go!! this call took 2)
2020-04-19 07:54:49.758  INFO 6512 --- [parallel-5] ch.keepcalm.demo.TurtleService: TurtleServiceResponse(message=[30]CallFromEventListener Ready, set, go!! this call took 2)
2020-04-19 07:54:49.758  INFO 6512 --- [ctor-http-nio-6] ch.keepcalm.demo.Client  : --> Client[5] :  [5]CallFromEventListener Ready, set, go!! this call took 2
```





The example source code can be found here [GitHub](https://github.com/marzelwidmer/kboot-resilience4j)



> **_References:_**  
>[Resilience4j docs](https://resilience4j.readme.io/docs)
 
