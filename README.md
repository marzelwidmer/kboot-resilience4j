# Reactive Spring Boot with Resilience4j CircuitBreaker

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

