import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route

import scala.io.StdIn

object HighLevelServer extends App {

  implicit val system: ActorSystem = ActorSystem("system")

  val simpleRoute: Route =
    path("home") {
      post {
        complete(StatusCodes.OK, HttpEntity(ContentTypes.`text/plain(UTF-8)`, "You hit the endpoint via POST"))
      } ~
        get {
          complete(StatusCodes.OK, HttpEntity(ContentTypes.`text/plain(UTF-8)`, "You hit the endpoint via GET"))
        }
    } ~
      path("about") {
        get {
          complete(StatusCodes.OK, HttpEntity(ContentTypes.`text/plain(UTF-8)`, "You hit the endpoint via GET"))
        }
      } ~
      path("nashtech" / "services") {
        get {
          complete(StatusCodes.OK)
        }
      } ~
      path("api" / "item" / IntNumber) { (number) =>
        println(s"I got item number as : $number")
        complete(StatusCodes.OK)
      } ~
      path("api" / "item") {
        parameter("id".as[Int]) { (id: Int) =>
          println(s"I got id as: $id in query parameter")
          complete(StatusCodes.OK)
        }
      }


  val server = Http().newServerAt("localhost", 9000).bind(simpleRoute)
  println("server is running of http://localhost:9000")
  StdIn.readLine()

}
