# prober Project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

Or:

```shell script
quarkus dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

Or:

```shell script
quarkus build
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/prober-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- RESTEasy Reactive ([guide](https://quarkus.io/guides/resteasy-reactive)): A JAX-RS implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.

## Provided Code

### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

## SMALLRYE HEALTH

<https://quarkus.io/guides/smallrye-health>

- <http://localhost:8080/q/health/live> - The application is up and running.
- <http://localhost:8080/q/health/ready> - The application is ready to serve requests.
- <http://localhost:8080/q/health/started> - The application is started.
- <http://localhost:8080/q/health> - Accumulating all health check procedures in the application.

## USING OPENAPI AND SWAGGER UI

<https://quarkus.io/guides/openapi-swaggerui>
<http://localhost:8080/q/swagger-ui>

## USING THE MONGODB CLIENT

<https://quarkus.io/guides/mongodb>
mongo-express: <http://localhost:8081/>

## REST API

Sample:

```json
{
    "name": "foo",
    "urls": [
        "https://google.com", "https://facebook.com"
    ],
    "intervalSeconds": 10,
    "timeoutSeconds": 10,
    "httpMethod": "GET"
}
```
