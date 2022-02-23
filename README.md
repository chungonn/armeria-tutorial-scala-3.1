# sbt project compiled with Scala 3

## Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

For more information on the sbt-dotty plugin, see the
[scala3-example-project](https://github.com/scala/scala3-example-project/blob/main/README.md).

## References
* [JOTB19 - Armeria: The Only Thrift/gRPC/REST Microservice Framework You'll Need by Trustin H. Lee](https://www.youtube.com/watch?v=hLlctum1pIA)
* [Scala 3 version of the tutorial](https://github.com/chungonn/armeria-tutorial-scala-3.1)
* [Armenia Website](https://armeria.dev/)

## How to set up the tutorial project?

### 1. Create a new Scala 3 project using Sbt
```
sbt new scala/scala3.g8
```

### 2. Add these lib dependencies
Below is the SBT setup:
```sbt
val scala3Version = "3.1.1"

lazy val root = project
 .in(file("."))
 .settings(
   name := "armeria-tutorial-blog-service",
   version := "0.1.0-SNAPSHOT",

   scalaVersion := scala3Version,

   libraryDependencies ++= Seq(
     "com.linecorp.armeria" %% "armeria-scala" % "1.14.0",
     "ch.qos.logback" % "logback-classic" % "1.2.10",
     "com.novocode" % "junit-interface" % "0.11" % "test"
   )
 )

```

### 3. Open project using Intellij and work on the tutorial
Refer to [https://armeria.dev/tutorials](https://armeria.dev/tutorials)

## REST Tutorial Introduction
Reference: [https://armeria.dev/tutorials/rest/blog](https://armeria.dev/tutorials/rest/blog)

### Sample File Structure
* project
* src
  * main
    * scala
      * server
        * blog
          * BadRequestExceptionHandler.scala
          * BlogPost.scala
          * BlogPostRequestConverter.scala
          * BlogService.scala
          * Main.scala
  * test
* target
* build.sbt

### Sample API Service
| Operation | Method                          | Annotation 
| --- |---------------------------------|------------|
| Create | `createBlogPost()`              | `@Post`    |
| Read | `getBlogPost()`, `getBlogPosts()` | `@Get`     |
| Update | `updateBlogPost()` | `@Put`     |
| Delete | `deleteBlogPost()` | `@Delete`     |

### Step 1: Creating a server

#### Create a server instance
1. Create a `Main` class in `server.blog`
2. Add logger 
3. Implement `newServer()` method

Main.scala:
```scala
package server.blog

import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.server.Server
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("BlogServer")

private def newServer(port: Int): Server =
 Server
   .builder()
   .http(port)
   .service("/", (ctx, req) => HttpResponse.of("Hello, Armeria!"))
   .build()
```

#### Start the server
1. In the `Main.scala` file, add the `main()` method. 
2. Call the `newServer()` method that was implemented above. Set the port to 8080.
3. Start the server: `server.start().join()` 

Main.scala: 
```scala
package server.blog

import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.server.Server
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("BlogServer")

/**
* https://armeria.dev/tutorials/rest/blog
*/
@main def start = {
  val server = newServer(8080)
  server.start().join()
  logger.info("Server has been started. Serving test service at http://127.0.0.1:{}", server.activeLocalPort())
}

private def newServer(port: Int): Server = ...
```

####Run the server and service
To run the server, go to your Intellij and open the `Main.scala`. Beside the `start()` method, click the green play icon.

![Run Server](docs/readme/Step1_RunServer.jpg)

Open the URL http://127.0.0.1:8080 on a web browser and check the message `Hello, Armeria!` is displayed on the page.

### Step 2: Prepare a data object

#### Create BlogPost class
```scala
package server.blog

object BlogPost {

 def apply(id: Int, title: String, content: String): BlogPost =
   apply(id, title, content, System.currentTimeMillis())

 def apply(id: Int, title: String, content: String, created: Long): BlogPost =
   BlogPost(id, title, content, created, created)

}

case class BlogPost(id: Int, title: String, content: String, createAt: Long, modifiedAt: Long)
```

### Step 3: Add services to a server

#### Create BlogService class

Create a scala file, `BlogService.scala` with variable `blogPosts`.
```scala
package server.blog

import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable.Map as MMap
import scala.jdk.CollectionConverters.*

case class BlogService(blogPosts: MMap[Int, BlogPost] = ConcurrentHashMap[Int, BlogPost]().asScala)
```

#### Add a blog service to server
In the `Main.scala`, remove the test service that was added in Step 1, and pass `BlogService()` instead.
```scala
private def newServer(port: Int): Server =
 Server
   .builder()
   .http(port)
   .annotatedService(BlogService())
   .build()
```

#### Add the Armeria’s Documentation service
In the `Main.scala`, add a `docService()` method and a request example for creating blog posts, using `DocServiceBuilder.exampleRequests()`.
```scala
private val docService =
 DocService
   .builder()
   .exampleRequests(BlogService.getClass, "createBlogPost", "{\"title\":\"My first blog\", \"content\":\"Hello Armeria!\"}")
   .build()
```

Inside the `newServer()` method, add the docService as a service and bind it to `/docs` path. 
```scala
private def newServer(port: Int): Server =
 Server
   .builder()
   .http(port)
   .annotatedService(BlogService())
   .serviceUnder("/docs", docService)  // Add Documentation service
   .build()
```

To access the documentation service result easily, edit the log message that was added in Step 1 to the one specified below.
```scala
@main def start = {
 val server = newServer(8080)
 server.start().join()
 logger.info(s"Server has been started. Serving DocService at http://127.0.0.1:8080/docs", server.activeLocalPort());
}
```

#### Run the server and service.
Go to http://127.0.0.1:8080/docs 

### Step 4: Implement CREATE

#### Map HTTP method
Declare a service method, `createBlogPost()`, in the class `BlogService`. Map this service method with the HTTP POST method by adding the `@Post` annotation, and bind the endpoint `/blogs` to the method.

```scala
package server.blog

import com.linecorp.armeria.server.annotation.Post

import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable.Map as MMap
import scala.jdk.CollectionConverters.*

case class BlogService(blogPosts: MMap[Int, BlogPost] = ConcurrentHashMap[Int, BlogPost]().asScala):

 @Post("/blogs")
 def createBlogPost(blogPost: BlogPost) = ???
```

#### Handle parameters
Armeria's request converter converts request parameters in HTTP messages into Java objects for you. In the request converter, we define what keys of a JSON object to map with what properties of a Java object.

#### Write a request converter
1. Create a `BlogPostRequestConverter.scala` file and declare a class, implementing the `RequestConverterFunction` interface.
``` scala
package server.blog

import com.fasterxml.jackson.databind.ObjectMapper
import com.linecorp.armeria.server.annotation.RequestConverterFunction

import java.util.concurrent.atomic.AtomicInteger

class BlogPostRequestConverter extends RequestConverterFunction:
 private val mapper = ObjectMapper()
 private val idGenerator = AtomicInteger()
```
2. Add a method retrieving a value of a given key in a JSON object:
```scala
package server.blog

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.linecorp.armeria.server.annotation.RequestConverterFunction

import java.util.concurrent.atomic.AtomicInteger

class BlogPostRequestConverter extends RequestConverterFunction:
 private val mapper = ObjectMapper()
 private val idGenerator = AtomicInteger()

 def stringValue(jsonNode: JsonNode, field: String): String =
   val value = jsonNode.get(field)
   if value == null then
     throw new IllegalArgumentException(field + " is missing")
   else
     value.textValue
```
3. Customize the default `convertRequest()` method as follows.
```scala
package server.blog

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.linecorp.armeria.common.AggregatedHttpRequest
import com.linecorp.armeria.server.ServiceRequestContext
import com.linecorp.armeria.server.annotation.RequestConverterFunction

import java.lang.reflect.ParameterizedType
import java.util.concurrent.atomic.AtomicInteger

class BlogPostRequestConverter extends RequestConverterFunction:
 private val mapper = ObjectMapper()
 private val idGenerator = AtomicInteger()

 def stringValue(jsonNode: JsonNode, field: String): String = ...

 override def convertRequest(ctx: ServiceRequestContext,
                             request: AggregatedHttpRequest, expectedResultType: Class[_],
                             expectedParameterizedResultType: ParameterizedType): AnyRef =
   if expectedResultType == classOf[BlogPost] then
     val jsonNode = mapper.readTree(request.contentUtf8())
     val id = idGenerator.getAndIncrement()
     val title = stringValue(jsonNode, "title")
     val content = stringValue(jsonNode, "content")
     BlogPost(id, title, content)
   else
     RequestConverterFunction.fallthrough()
```

#### Register a request converter
Assign the request converter we customized to our service method. Annotate the service method with `@RequestConverter` and specify the `RequestConverterFunction` class as `BlogPostRequestConverter.class`.
```scala
case class BlogService(blogPosts: MMap[Int, BlogPost] = ConcurrentHashMap[Int, BlogPost]().asScala):

 @Post("/blogs")
 @RequestConverter(classOf[BlogPostRequestConverter])
 def createBlogPost(blogPost: BlogPost) = ???
```

#### Test creating a blog post
1. Run the server like we did in Step 1 by running the `main()` method. When you see the message, "Server has been started", you can try testing the service method.
2. Call the service method for creating a blog post. Here, we'll use cURL.
```shell
curl --request POST 'localhost:8080/blogs' \
-H 'Content-Type: application/json' \
-d '{"title":"My first blog", "content":"Hello Armeria!"}'
```
3. Check the return value. The response includes created and modified times.
```json
{
  "id": 0,
  "title": "My first blog",
  "content": "Hello Armeria!",
  "createdAt": ...,
  "modifiedAt": ...
}
```

### Step 5: Implement READ

#### Get a single blog post
```scala
case class BlogService(blogPosts: MMap[Int, BlogPost] = ConcurrentHashMap[Int, BlogPost]().asScala):

 ...

 @Get("/blogs/:id")
 def getBlogPost(@Param id: Int): HttpResponse =
   val blogPost = blogPosts.get(id)
   HttpResponse.ofJson(blogPost)
```

Test command to retrieve the blog post by creating 2 blog posts. 
 
```shell
curl --request POST 'localhost:8080/blogs' \
-H 'Content-Type: application/json' \
-d '{"title":"First post for testing", "content":"Test reading."}'
```
```shell
curl --request POST 'localhost:8080/blogs' \
-H 'Content-Type: application/json' \
-d '{"title":"Second post for testing", "content":"Test reading a post."}'
```

#### Get specific post
```shell
curl --request GET 'localhost:8080/blogs/1'
```
```json
{
  "id": 1,
  "title": "Second post for testing",
  "content": "Test reading a post.",
  "createAt": 1644836065226,
  "modifiedAt": 1644836065226
}
```

#### Get a multiple blog posts
```scala
case class BlogService(blogPosts: MMap[Int, BlogPost] = ConcurrentHashMap[Int, BlogPost]().asScala):
 ...

 @Get("/blogs")
 @ProducesJson
 def getBlogPosts(@Param @Default("true") descending: Boolean): Iterable[BlogPost] =
   if descending then
     blogPosts.toList.sortBy(_._1).reverse.map(_._2)
   else
     blogPosts.values
```

Test command to retrieve the blog post by creating 2 blog posts.

```shell
curl --request POST 'localhost:8080/blogs' \
-H 'Content-Type: application/json' \
-d '{"title":"First post for testing", "content":"Test reading."}'
```

```shell
curl --request POST 'localhost:8080/blogs' \
-H 'Content-Type: application/json' \
-d '{"title":"Second post for testing", "content":"Test reading a post."}'
```

Get all blog posts
```shell
curl --request GET 'localhost:8080/blogs'
```
```json
[
  {
    "id": 1,
    "title": "Second post for testing",
    "content": "Test reading a post.",
    "createAt": 1644836065226,
    "modifiedAt": 1644836065226
  },
  {
    "id": 0,
    "title": "First post for testing",
    "content": "Test reading.",
    "createAt": 1644836055327,
    "modifiedAt": 1644836055327
  }
]
```

### Step 6: Implement UPDATE
In `BlogPost.scala`, add a constructor for JSON creation.
```scala
package server.blog

import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty}

...

case class BlogPost(id: Int, title: String, content: String, createAt: Long, modifiedAt: Long):
 @JsonCreator
 def this(@JsonProperty("id") id: Int, @JsonProperty("title") title: String,
          @JsonProperty("content") content: String) =
   this(id, title, content, System.currentTimeMillis(), System.currentTimeMillis())
```

Create an `updateBlogPost()` method that takes in an ID and blogPost. 
```scala
import com.linecorp.armeria.common.{HttpResponse, HttpStatus}
import com.linecorp.armeria.server.annotation.{Default, Get, Param, Post, ProducesJson, Put, RequestConverter, RequestObject}

import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable.Map as MMap
import scala.jdk.CollectionConverters.*

case class BlogService(blogPosts: MMap[Int, BlogPost] = ConcurrentHashMap[Int, BlogPost]().asScala):
 ...

 @Put("/blogs/:id")
 def updateBlogPost(@Param id: Int, @RequestObject blogPost: BlogPost): HttpResponse =
   val oldBlogPost = blogPosts.get(id).orNull
   if oldBlogPost == null then
     HttpResponse.of(HttpStatus.NOT_FOUND)
   else
     val newBlogPost = blogPost.copy(id = id, createAt = oldBlogPost.createAt)
     blogPosts.put(id, newBlogPost)
     HttpResponse.ofJson(newBlogPost)
```

#### Test updating a blog post
1. Run the server like we did in Step 1 by running the `main()` method. When you see the message, "Server has been started", you can try testing service methods.
2. Create a couple of blog posts to test updating a blog post and get the ID value returned. Enter the cURL commands below.
```shell
curl --request POST 'localhost:8080/blogs' \
-H 'Content-Type: application/json' \
-d '{"title":"First post for testing", "content":"Test updating."}'
```
```shell
curl --request POST 'localhost:8080/blogs' \
-H 'Content-Type: application/json' \
-d '{"title":"Second post for testing", "content":"Test updating a post."}'
```
3. Update the second blog with ID #1. Change the value for the title and content properties.
```shell
curl --request PUT 'localhost:8080/blogs/1' \
-H 'Content-Type: application/json' \
-d '{
    "title": "Update title to this title",
    "content": "Update blog content with this content."
}'
```
Sample Response:
```json
{
  "id": 1,
  "title": "Update title to this title",
  "content": "Update blog content with this content.",
  "createAt": 1644836629508,
  "modifiedAt": 1644836700605
}

```
4. Retrieve the same blog post to see the change
```json
{
  "id": 1,
  "title": "Update title to this title",
  "content": "Update blog content with this content.",
  "createAt": 1644836629508,
  "modifiedAt": 1644836700605
}

```

### Step 7: Implement DELETE

#### Create an exception handler for the blog service. 
1. Create a file, `BadRequestExceptionHandler.scala`
2. In the file, declare the custom exception handler implementing Armeria’s `ExceptionHandlerFunction` interface
3. Implement your own exception handler by overriding the default `handleException()` method. Add a code block for handling the `IllegalArgumentException` thrown. For this tutorial, return a `BAD REQUEST` as the response.
4. Assign the exception handler to the `deleteBlogPost()` method by annotating the `deletePost()` method with the `@ExceptionHandler` as follows.

```scala
package server.blog

import com.fasterxml.jackson.databind.ObjectMapper
import com.linecorp.armeria.common.{HttpRequest, HttpResponse, HttpStatus}
import com.linecorp.armeria.server.ServiceRequestContext
import com.linecorp.armeria.server.annotation.ExceptionHandlerFunction

class BadRequestExceptionHandler extends ExceptionHandlerFunction :
 val mapper = ObjectMapper()

 override def handleException(ctx: ServiceRequestContext, req: HttpRequest, cause: Throwable): HttpResponse =
   if cause.isInstanceOf[IllegalArgumentException] then
     val message = cause.getMessage
     val objectNode = mapper.createObjectNode()
     objectNode.put("error", message)
     HttpResponse.ofJson(HttpStatus.BAD_REQUEST, objectNode)
   else
     ExceptionHandlerFunction.fallthrough()
```

#### Add `deleteBlogPost()` method in `BlogService.scala`

```scala
package server.blog

import com.linecorp.armeria.common.{HttpResponse, HttpStatus}
import com.linecorp.armeria.server.annotation.{Blocking, Default, Delete, ExceptionHandler, Get, Param, Post, ProducesJson, Put, RequestConverter, RequestObject}

import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable.Map as MMap
import scala.jdk.CollectionConverters.*

case class BlogService(blogPosts: MMap[Int, BlogPost] = ConcurrentHashMap[Int, BlogPost]().asScala):
 ...

 @Blocking
 @Delete("/blogs/:id")
 @ExceptionHandler(classOf[BadRequestExceptionHandler])
 def deleteBlogPost(@Param id: Int): HttpResponse =
   val removed = blogPosts.remove(id).orNull
   HttpResponse.of(HttpStatus.NO_CONTENT)
```

The `@Blocking` annotation is to specify that the annotated service method must be invoked from the block task executor instead of the event loop thread. (Reference: https://armeria.dev/docs/server-annotated-service/#specifying-a-blocking-task-executor)

The `@ExceptionHandler` annotation is to transform exceptions into a response.

#### Test deleting a blog post

1. Run the server like we did in Step 1 by running the `main()` method. When you see the message, "Server has been started", you can try testing service methods.
2. Create a couple of blog posts to test deleting a blog post and get the ID value returned. Enter the cURL commands below.
```shell
curl --request POST 'localhost:8080/blogs' \
-H 'Content-Type: application/json' \
-d '{"title":"First post for testing", "content":"Test deletion."}'
```
```shell
curl --request POST 'localhost:8080/blogs' \
-H 'Content-Type: application/json' \
-d '{"title":"Second post for testing", "content":"Test deleting a post."}'
```
3. Delete the second blog with the ID, 0. Since our service method doesn't return anything, you won't see any response.
```shell
curl --request DELETE 'localhost:8080/blogs/1'
```
4. Retrieve the blog post list to see if the post has been deleted successfully.
```shell
curl --request GET 'localhost:8080/blogs'
```
Response:
```json
[
  {
    "id": 0,
    "title": "First post for testing",
    "content": "Test deletion.",
    "createAt": 1644837558821,
    "modifiedAt": 1644837558821
  }
]

```