# RestClient - [Unirest](https://github.com/Mashape/unirest-java) fork

This fork is intended to fix the bugs left by the original authors, improve the API, and provide continuous support.
Improvements, new ideas, and bug reports are always welcome.

[![License][license-image]][license-url]

## Features

Apart from the features provided by the original Unirest Java, this fork also has:

* Updated API to make use of Java 8
* Major bug fixes
* Support for multiple independent clients
* General API improvements
* Updated async requests to use Java 8 CompletableFuture
* Lazy response body parsing
* Default Gson parser
* TLS certificate and hostname verification enabled by default


### Maven

```xml
<dependency>
    <groupId>io.joshworks.unirest</groupId>
    <artifactId>unirest-java</artifactId>
    <version>1.8.0</version>
</dependency>
```

### Creating a new client with defaults
The following example creates a new basic RestClient instance. At the moment, each client will have its own 
HttpClient sync and async client.

```java

RestClient client = RestClient.builder().build();

```

## Creating Request

```java

RestClient client = RestClient.builder().build();

HttpResponse<Json> jsonResponse = client.post("http://httpbin.org/post")
  .header("accept", "application/json")
  .queryString("apiKey", "123")
  .asJson();
```

### Reading the response

`asString()`, `asJson()`, `asObject(...)` and `asBinary()` all return an
`HttpResponse`. The body is parsed lazily, so it can be read in the
representation that is most convenient for the response.

```java

HttpResponse<String> response = client.get("http://my-api.com/v1/users").asString();

int status = response.status();
boolean successful = response.successful();
Headers headers = response.headers();
String body = response.body();

```

Binary responses expose an `InputStream` and must be closed to release the
connection back to the pool:

```java

try (HttpResponse<InputStream> response = client.get("http://my-api.com/file").asBinary()) {
    // consume response.body()
}

```

### Base url

```java

RestClient client = RestClient.builder().baseUrl("http://my-api.com/v1").build();
String response = client.get("/some-resource").asString().body();

```

### Unirest client
Unirest provides the same static methods as the original version. It's ideal for simple usage with default configuration. 

```java

String response = Unirest.get("http://my-api.com/v1").asString().body();

```

### Path parameters
With the new API, there's no need to concatenate path parameters, making the it easier and much more convenient to use.
You can either provide a varargs path. Or you can use a template and then use `.routeParam()` to specify its values.
```java

//Sends a request to http://my-api.com/v1/users
String response = client.post("http://my-api.com", "v1", "users").asString().body();

//Alternatively

//Also Sends a request to http://my-api.com/v1/users
String response = client.get("http://my-api.com/{version}/{endpoint}")
  .routeParam("version", "v1")
  .routeParam("endpoint", "users")
  .asString()
  .body();

```


### Async requests with CompletableFuture
When using asynchronous requests you can use Java 8 CompletableFuture to handle the response.
This also gives you the ability to compose multiple requests in a convenient way. 

```java
client.get("http://my-api.com/v1/hello")
        .asStringAsync()
        .thenAccept(resp -> {
            System.out.println(resp.body());
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
JSON is supported out of the box by `asJson()`, `asObject(Class)` and `.body(Object)`.
For other content types, register a custom `ObjectMapper` implementation.
Mappers are registered globally and can be overridden per media type.
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

### TLS

TLS certificates and hostnames are verified by default. To use a custom trust
store, provide an `SSLContext`:

```java

SSLContext sslContext = SSLContextBuilder.create()
        .loadTrustMaterial(new File("my-truststore.p12"), "changeit".toCharArray())
        .build();

RestClient client = RestClient.builder()
        .sslContext(sslContext)
        .build();

```

`disableSSLCheck()` disables both certificate and hostname verification. It
makes the client vulnerable to man-in-the-middle attacks and should only be
used for local development or tests:

```java

// Do not use in production
RestClient client = RestClient.builder().disableSSLCheck().build();

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
client.close();

//When your application is shutting down:
//Closes all client connections and the monitor
ClientContainer.shutdown();

```

[license-url]: https://github.com/joshgontijo/rest-client/blob/master/LICENSE
[license-image]: https://img.shields.io/badge/license-MIT-blue.svg?style=flat
