import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.util.Timeout
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

case class Mobile(name: String, model: String)

case class MobileResponse(name: String, model: String, id: Int)

object MobileDb {
  case class CreateMobile(mobile: Mobile)

  case class CreatedMobile(id: Int)

  case object GetAllMobiles

  case class GetMobileById(id: Int)
}

class MobileDb extends Actor with ActorLogging {

  import MobileDb._

  private var mobiles: Map[Int, Mobile] = Map.empty
  private var currentMobileId = 0

  override def receive: Receive = {
    case CreateMobile(mobile) =>
      mobiles = mobiles + (currentMobileId -> mobile)
      sender() ! CreatedMobile(currentMobileId)
      currentMobileId = currentMobileId + 1
    case GetAllMobiles =>
      log.info("Searching for all mobiles!")
      sender() ! mobiles.values.toList
    case GetMobileById(id) =>
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

  private val mobileDbActor = system.actorOf(Props[MobileDb], "mobiledb")
  private val listOfMobiles = List(
    Mobile("SAMSUNG", "M13"),
    Mobile("MOTO", "G84"),
  )

  listOfMobiles.foreach { m =>
    mobileDbActor ! CreateMobile(m)
  }

  implicit val defaultTimeout: Timeout = Timeout(2 seconds)

  private def getMobile(query: Uri.Query): Future[HttpResponse] = {
    val mobileId = query.get("id")

    mobileId match {
      case Some(id) => Try(id.toInt) match {
        case Failure(_) => Future(HttpResponse(StatusCodes.NoContent))
        case Success(value) =>
          val mobileFuture = (mobileDbActor ? GetMobileById(value)).mapTo[Option[Mobile]]
          mobileFuture.map {
            case Some(value) => HttpResponse(
              StatusCodes.OK,
              entity = HttpEntity(
                ContentTypes.`application/json`,
                value.toJson.prettyPrint
              )
            )
            case None => HttpResponse(StatusCodes.NoContent)
          }
      }
      case None => Future(HttpResponse(StatusCodes.NoContent))
    }
  }


  private val httpRequestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/mobile"), _, _, _) =>
      val query = uri.query()
      if (query.isEmpty) {
        val allMobiles = (mobileDbActor ? GetAllMobiles).mapTo[List[Mobile]]

        for {
          mob <- allMobiles
        } yield HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(
            ContentTypes.`application/json`,
            mob.toJson.prettyPrint
          )
        )
      } else {
        getMobile(query)
      }
    case HttpRequest(HttpMethods.POST, Uri.Path("/api/mobile"), _, entity, _) =>

      val strictFuture = entity.toStrict(2 seconds)
      strictFuture.flatMap { strictEntity =>
        val mobileJsonString = strictEntity.data.utf8String
        println(s"Received data from client: $mobileJsonString")
        val mobile = mobileJsonString.parseJson.convertTo[Mobile]
        val mobileCreated = (mobileDbActor ? CreateMobile(mobile)).mapTo[CreatedMobile]
        for {
          mob <- mobileCreated
        } yield {
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`text/plain(UTF-8)`,
              mob.id.toString
            )
          )
        }
      }

    case request: HttpRequest =>
      request.discardEntityBytes()
      Future {
        HttpResponse(status = StatusCodes.OK)
      }

  }

  private val futureRequest = Http().newServerAt("localhost", 8090).bind(httpRequestHandler)

  println(s"Server online at http://localhost:8090/")
  StdIn.readLine()

  futureRequest
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}

// THE OTHER WAY TO CREATE RESPONSE ON THE BASIS OF THE ENTITY BODY
//      entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body =>
//        val requestData = body.utf8String
//        val caseClassMobile = requestData.parseJson.convertTo[Mobile]
//        mobileDbActor ! CreateMobile(caseClassMobile)
//        println(s"Received data from client: $requestData")
//        HttpResponse(StatusCodes.OK)
//      }

// converting to json OR MARSHALLING
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

// converting to case class OR UNMARSHALLING
//  val mobileCaseClass = mobileStringJson.parseJson.convertTo[Mobile]
