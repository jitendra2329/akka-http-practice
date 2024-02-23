import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import spray.json._
import scala.io.StdIn
import scala.util.{Failure, Success, Try}

trait RequestError
case class InvalidId(message: String) extends RequestError

trait ErrorProtocol extends DefaultJsonProtocol {
  implicit val errorFormat: RootJsonFormat[InvalidId] = jsonFormat1(InvalidId)
}
object HighLevelServer extends App with ErrorProtocol {

  implicit val system: ActorSystem = ActorSystem("system")

  private def validateRequest(id: String): Either[RequestError, String] = {
    Try(id.toInt) match {
      case Failure(_) =>  Left(InvalidId("Id must me a number!"))
      case Success(value) =>
        println(s"I got id as: $value in query parameter")
        Right("Request successful!")
    }
  }

  private val simpleRoute: Route =
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
        parameter("id") { (id: String) =>
          validateRequest(id) match {
            case Left(value: InvalidId) =>  complete(
              StatusCodes.BadRequest,
              HttpEntity(
                ContentTypes.`application/json`,
                value.toJson.prettyPrint
              )
            )
            case Right(message) => complete(
              StatusCodes.OK,
              HttpEntity(
                ContentTypes.`text/plain(UTF-8)`,
                message
              )
            )
          }
        }
      }


  val server = Http().newServerAt("localhost", 9000).bind(simpleRoute)
  println("server is running of http://localhost:9000")
  StdIn.readLine()

}
