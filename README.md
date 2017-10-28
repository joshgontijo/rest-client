# RestClient - [Unirest](https://github.com/Mashape/unirest-java) fork


[Unirest](http://unirest.io) is a set of lightweight HTTP libraries available in multiple languages, built and maintained by [Mashape](https://github.com/Mashape), who also maintain the open-source API Gateway [Kong](https://github.com/Mashape/kong). 



[![License][license-image]][license-url]


## Features

Apart from the features provided by Unirest Java, this fork also provides:

* Bug fixes
* Independent client configuration
* New fluent API for async requests
* Lazy response body parsing
* Default JsonMapper using Gson
* New fluent async API
* Single idle thread monitor for all clients


### Maven


```xml
<dependency>
    <groupId>io.joshworks.unirest</groupId>
    <artifactId>unirest-java</artifactId>
    <version>1.5.0</version>
</dependency>
```

## Basics

Please read the [Unirest Documentation](https://github.com/Mashape/unirest-java) for basic examples on how to use the core api.
This documentation aims to show the additional features on top of the library.


### Creating a new client with defaults
The following example creates a new basic RestClient. At the moment, each client will have its own 
HttpClient sync and async client.

```java

RestClient client = RestClient.newClient().build();

```

### Base url

```java

RestClient client = RestClient.newClient().baseUrl("http://my-api.com/v1").build();
String response = client.get("/some-resource").asString();

```

### Unirest client
Unirest provides static methods for simple usage with default configuration.

```java

String response = Unirest.get("http://my-api.com/v1").asString();

```


### Async fluent API

```java
client.get("http://my-api.com/v1/hello")
        .async(String.class)
        .completed(resp -> System.out.println(resp.getBody()))
        .failed((e) -> e.printStackTrace())
        .request();
```


### Serialization
Before an `asObject(Class)` or a `.body(Object)` invokation, is necessary to provide a custom implementation of the `ObjectMapper` interface.
This should be done for each client.

For example, serializing Json from / to Object using the popular Gson takes only few lines of code.
By default Gson is used, so there's no need to register any other unless you need custom configuration.


# Configuration API
Use the configuration api to configure a single client instance.

```java
        
        RestClient.newClient()
            .defaultHeader(String key, String value)
            .defaultHeader(String key, long value)
        
            .httpClient(CloseableHttpClient httpClient)
            .asyncHttpClient(CloseableHttpAsyncClient asyncHttpClient)
            .followRedirect(boolean followRedirect)
            .proxy(HttpHost proxy)

            .objectMapper(ObjectMapper objectMapper)
      
            .timeout(int connectionTimeout, int socketTimeout)
            .concurrency(int maxTotal)
```

# Exiting an application

RestClient starts a background idle thread monitor, which is a daemon thread. 
Always close the clients on application exit.

```java
//If a client is no longer needed and you want to dispose its resources
client.shutdown();

//When your application is shutting down:
//Closes all client connections and the monitor
ClientContainer.shutdown();

```

[license-url]: https://github.com/josueeduardo/rest-client/blob/master/LICENSE
[license-image]: https://img.shields.io/badge/license-MIT-blue.svg?style=flat
