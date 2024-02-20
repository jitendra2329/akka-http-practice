
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.Future
import spray.json._

import scala.io.StdIn
import scala.language.postfixOps

case class Mobile(name: String, model: String)

object MobileDb {
  case class createMobile(mobile: Mobile)

  case class createdMobile(id: Int)

  case object getAllMobiles

  case class findMobile(id: Int)
}

class MobileDb extends Actor with ActorLogging {

  import MobileDb._

  var mobiles: Map[Int, Mobile] = Map.empty
  var currentMobileId = 0

  override def receive: Receive = {
    case createMobile(mobile) =>
      mobiles = mobiles + (currentMobileId -> mobile)
      sender() ! createdMobile(currentMobileId)
      currentMobileId = currentMobileId + 1
    case getAllMobiles =>
      log.info("Searching for all mobiles!")
      sender() ! mobiles.values.toList
    case findMobile(id) =>
      log.info(s"Searching mobile by id: $id!")
      sender() ! mobiles.get(id)
  }
}

trait MobileFormatProtocol extends DefaultJsonProtocol {
  implicit val mobileFormat: RootJsonFormat[Mobile] = jsonFormat2(Mobile)
}

object MobileStore extends App with MobileFormatProtocol {
  import MobileDb._

  implicit val system: ActorSystem = ActorSystem("actorSystem")
  import system.dispatcher

  val mobileDbActor = system.actorOf(Props[MobileDb], "mobiledb")
  val listOfMobiles = List(
    Mobile("SAMSUNG", "M13"),
    Mobile("MOTO", "G84"),
  )

  listOfMobiles.foreach{ m =>
    mobileDbActor ! createMobile(m)
  }

  implicit val defaultTimeout: Timeout = Timeout(2 seconds)

  private val httpRequestHandler: HttpRequest=> Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/api/mobile"), _, _, _) =>
      val allMobiles = (mobileDbActor ? getAllMobiles).mapTo[List[Mobile]]
      allMobiles.map { mob =>
        HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(
            ContentTypes.`application/json`,
            mob.toJson.prettyPrint
          )
        )
      }

    case request: HttpRequest =>
              request.discardEntityBytes()
              Future{
                HttpResponse(status = StatusCodes.OK)
              }
//    case HttpRequest(HttpMethods.POST, Uri.Path("/api/mobile"), _, _, _) =>
//      Future(HttpResponse(
//        StatusCodes.OK,
//      ))
  }

  // converting to json OR marshaling
//  val simpleMobile = Mobile("SAMSUNG", "M13")
//  val mobileJson = simpleMobile.toJson
//
//
//  val mobileStringJson =
//    """
//      |{
//      |  "name" : "SAMSUNG",
//      |  "model" : "M13"
//      |}
//      |""".stripMargin

  // converting to case class OR unmarshalling
//  val mobileCaseClass = mobileStringJson.parseJson.convertTo[Mobile]

  private val futureRequest = Http().newServerAt("localhost", 8090).bind(httpRequestHandler)

  println(s"Server online at http://localhost:8090/")
  StdIn.readLine()

  futureRequest
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())

}
