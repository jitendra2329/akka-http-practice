
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._

import scala.concurrent.Future
import scala.io.StdIn

object HttpServer extends App {

    implicit val system = ActorSystem(Behaviors.empty, "my-system")

    implicit val executionContext = system.executionContext

    val route =
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      }

//    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

//    println(s"Server now online. Please navigate to http://localhost:8080/hello\nPress RETURN to stop...")
//    StdIn.readLine() // let it run until user presses return
//    bindingFuture
//      .flatMap(_.unbind()) // trigger unbinding from the port
//      .onComplete(_ => system.terminate()) // and shutdown when done


    private val httpRequest: HttpRequest => Future[HttpResponse] = {
      case HttpRequest(HttpMethods.GET, Uri.Path("/"), _, _, _) =>
        Future(HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Welcome to the front door.</h1>")
        ))

      case HttpRequest(HttpMethods.GET, Uri.Path("/About"), _, _, _) =>
        Future(HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>You are on the About page!</h1>")
        ))

      case HttpRequest(HttpMethods.GET, Uri.Path("/a"), _, _, _) =>
        Future(HttpResponse(
          StatusCodes.Found,
          headers = List(Location("www.google.com"))
        ))

      case _: HttpRequest => Future(HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Page not found!</h1>")
      ))
    }

    private val bindingFuture2 = Http().newServerAt("localhost", 8081).bind(httpRequest)

    println(s"Server online at http://localhost:8081/")

    StdIn.readLine() // let it run until user presses return
    bindingFuture2
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done

  }
