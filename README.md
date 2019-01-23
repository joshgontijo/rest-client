# RestClient - [Unirest](https://github.com/Mashape/unirest-java) fork


[Unirest](http://unirest.io) is a set of lightweight HTTP libraries available in multiple languages, built and maintained 
by [Mashape](https://github.com/Mashape), who also maintain the open-source API Gateway 
[Kong](https://github.com/Mashape/kong).

This fork is intended to fix the bugs left by the original authors, improve the API, and provide continuous support.
Improvements, new ideas, and bug reports are very welcome. 



[![License][license-image]][license-url]


## Features

Apart from the features provided by the original Unirest Java, this fork also provides:

* Updated API to make use of Java 8
* Major Bug fixes
* Support for multiple independent clients
* General API improvements
* Updated async requests to use Java 8 CompletableFuture
* Lazy response body parsing
* Default Gson parser 


### Maven

```xml
<dependency>
    <groupId>io.joshworks.unirest</groupId>
    <artifactId>unirest-java</artifactId>
    <version>1.7.0</version>
</dependency>
```

## Basics

Please read the [Unirest Documentation](https://github.com/Mashape/unirest-java) for basic examples on how to use the core api.
This documentation aims to show the additional features and improvements from the original library.


### Creating a new client with defaults
The following example creates a new basic RestClient instance. At the moment, each client will have its own 
HttpClient sync and async client.

```java

RestClient client = RestClient.builder().build();

```

### Base url

```java

RestClient client = RestClient.builder().baseUrl("http://my-api.com/v1").build();
String response = client.get("/some-resource").asString();

```

### Unirest client
Unirest provides the same static methods as the original version. It's ideal for simple usage with default configuration. 

```java

String response = Unirest.get("http://my-api.com/v1").asString();

```

### Path parameters

```java

//Send a request to http://locahost:9000/v1/users/josh
String response = client.post("http://locahost:9000", "v1", "users", "josh").asString();

```

### Async requests with CompletableFuture
When using asynchronous requests you can use Java 8 CompletableFuture to handle the response.
This also gives you the ability to compose multiple requests in a convenient way. 

```java
client.get("http://my-api.com/v1/hello")
        .asStringAsync()
        .thenAccept(resp -> {
            System.out.println(resp.body())
         })
         .exceptionally(e -> {
            e.printStackTrace();
            return null;
         });
         
```

### New multipart/form-data and x-www-form-urlencoded API
The new API for form data makes easier to specify the right values for each type of request. When using `.part(...)` a 
`multipart/form-data` request will be sent, `.field(...)` will create `x-www-form-urlencoded` request. This makes the interface cleaner and less error prone.
The content type is also set automatically.

```java
//multipart
client.post("http://my-service.com/fileUpload")
        .part("param3", value)
        .part("file", new File("test.txt"))
        .asJson();

//form-urlencoded
client.post("http://my-service.com/login")
                .field("username", "admin")
                .field("password", "admin123")
                .asJson();

```

### Serialization
Before using `asObject(Class)` or `.body(Object)`, is necessary to provide a custom implementation of the `ObjectMapper` interface.
This should be done for each client.
Json is supported out-of-the-box, so there's no need to register any other json mapper unless you want a custom configuration.
Here's how to configure a new ObjectMapper:

```java
public class XmlMapper implements ObjectMapper {
   
    @Override
    public <T> T readValue(String value, Class<T> valueType) {
        //...
    }

    @Override
    public String writeValue(Object value) {
        //...
    }
}

ObjectMappers.register(MediaType.APPLICATION_XML_TYPE, new XmlMapper());


HttpResponse<User> response = client.post("http://my-api.com/echo-xml")
                .header("Accept", "application/xml")
                .header("Content-Type", "application/xml")
                .body(new User("John"))
                .asObject(User.class);

```

### Client stats
The API also exposes HttpClient's PoolStats, so you can inspect the usage of each client.

```java
   
   ClientStats stats = client.stats();
   int leased = stats.sync.getLeased();
   int available = stats.sync.getAvailable();
   int max = stats.sync.getMax();
   int pending = stats.sync.getPending();

   //All clients
   Map<String, ClientStats> allStats = ClientContainer.stats();
   //...

```

# Exiting an application

RestClient starts a background idle thread monitor, which is a daemon thread. 
When exiting the application, you can use the `ClientContainer` to release all the allocated resources, as follows:

```java
//If a client is no longer needed and you want to dispose its resources
client.shutdown();

//When your application is shutting down:
//Closes all client connections and the monitor
ClientContainer.shutdown();

```

[license-url]: https://github.com/josueeduardo/rest-client/blob/master/LICENSE
[license-image]: https://img.shields.io/badge/license-MIT-blue.svg?style=flat
