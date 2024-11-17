# Request Handler Library

**RequestHandler** is a Java library for handling HTTP-like requests. It allows the mapping of request paths to controller methods using annotations, supporting both synchronous and asynchronous execution. The library simplifies request handling for applications that follow a controller-based structure.

## Features

 - Annotation-based request mapping: easily map request paths to methods using `@RequestMapping`.
 - Support for path and controller parameters: pass parameters through the request path or as additional controller parameters.
 - Synchronous and asynchronous handling: execute requests in blocking or non-blocking modes.
 - Request building: Use `RequestBuilder` to construct complex request paths with parameters.
 
## Usage
 
### 1. Annotate Controller Methods

Define a controller and annotate methods with `@RequestMapping` to specify their request paths:

```java
public class Controller {
    @RequestMapping(path = "/greet")
    public String greetUser() {
        return "Hello, User!";
    }

    @RequestMapping(path = "/sum")
    public int sum(@RequestParam(name = "a") int a, @RequestParam(name = "b") int b) {
        return a + b;
    }
}
```

### 2. Register Controllers

Create an instance of `RequestHandler` and register your controllers:

```java
RequestHandler requestHandler = new RequestHandler();
requestHandler.registerController(new Controller());
```

### 3. Handle Requests

Synchronously:

```java
String response = (String) requestHandler.handleRequest("/greet");
System.out.println(response);
int sum = (int) requestHandler.handleRequest("/sum/a=5/b=10");
System.out.println(sum);
```

Asynchronously:

```java
requestHandler.handleAsyncRequest("/greet",
    result -> System.out.println("Async result: " + result),
    error -> System.err.println("Async error: " + error)
);
```

### 4. Build Requests

Use `RequestBuilder` to construct request paths:

```java
String request = new RequestBuilder()
    .path("sum")
    .parameter("a", 5)
    .parameter("b", 10)
    .build();
```

## Annotations

 - `@RequestMapping` - maps a method or class to a specific request path.
 - `@RequestParam` - marks a method parameter as a request path parameter.
 - `@ControllerParam` - marks a method parameter as a controller parameter (used for additional data).
 
## Exception Handling

`RequestHandlerException` is thrown for errors in request handling, such as missing controllers, incorrect parameters, or invocation failures.