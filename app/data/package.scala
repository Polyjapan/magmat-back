import java.sql.PreparedStatement

import anorm.Macro.ColumnNaming
import anorm.{Column, Macro, RowParser, ToStatement}
import ch.japanimpact.auth.api.UserProfile
import org.joda.time.DateTime
import play.api.libs.json._

package object data {

  case class StorageLocation(storageLocationId: Option[Int], inConv: Boolean, room: String, space: Option[String], location: Option[String])

  case class ObjectType(objectTypeId: Option[Int], name: String, description: Option[String], storageLocation: Option[Int],
                        inconvStorageLocation: Option[Int], partOfLoan: Option[Int], requiresSignature: Boolean)

  case class CompleteObjectType(objectType: ObjectType, storageLocationObject: Option[StorageLocation],
                                inconvStorageLocationObject: Option[StorageLocation], partOfLoanObject: Option[CompleteExternalLoan])

  case class ExternalLender(externalLenderId: Option[Int], name: String, description: Option[String], phoneNumber: String,
                            email: String, location: String)

  object ObjectStatus extends Enumeration {
    type ObjectStatus = Value
    val InStock, Out, Lost, Resting, Deleted = Value
  }

  object LoanStatus extends Enumeration {
    type LoanStatus = Value
    val AwaitingPickup, AwaitingReturn, Returned = Value
  }


  case class ObjectLog(objectId: Int, eventId: Int, timestamp: DateTime, changedBy: Int, user: Int,
                       sourceState: ObjectStatus.Value, targetState: ObjectStatus.Value, signature: Option[String])

  case class ObjectLogWithUser(objectLog: ObjectLog, changedBy: UserProfile, user: UserProfile)

  case class ObjectLogWithObject(objectLog: ObjectLog, `object`: SingleObject, objectType: ObjectType)

  case class ObjectComment(objectId: Int, eventId: Int, timestamp: DateTime, writer: Int, comment: String)

  case class CompleteObjectComment(objectComment: ObjectComment, writer: UserProfile)

  case class SingleObject(objectId: Option[Int], objectTypeId: Int, suffix: String, description: Option[String],
                          storageLocation: Option[Int], inconvStorageLocation: Option[Int], partOfLoan: Option[Int],
                          reservedFor: Option[Int], assetTag: Option[String], status: ObjectStatus.Value, plannedUse: Option[String] = None, depositPlace: Option[String] = None)

  case class CompleteObject(`object`: SingleObject, objectType: ObjectType,
                            storageLocationObject: Option[StorageLocation],
                            inconvStorageLocationObject: Option[StorageLocation],
                            partOfLoanObject: Option[CompleteExternalLoan],
                            reservedFor: Option[UserProfile] = None)

  case class Event(eventId: Int, eventName: String, inConv: Boolean)

  case class ExternalLoan(externalLoanId: Option[Int], externalLenderId: Int, eventId: Int, pickupTime: DateTime,
                          returnTime: DateTime, loanDetails: Option[String], pickupPlace: Option[String], returnPlace: Option[String],
                          status: LoanStatus.Value)

  case class CompleteExternalLoan(externalLoan: ExternalLoan, event: Option[Event], lender: ExternalLender)

  object CompleteExternalLoan {
    def merge(externalLoan: ExternalLoan, event: Option[Event], lender: ExternalLender) =
      CompleteExternalLoan(
        externalLoan.copy(
          pickupPlace = externalLoan.pickupPlace.orElse(Some(lender.location)),
          returnPlace = externalLoan.returnPlace.orElse(externalLoan.pickupPlace).orElse(Some(lender.location))
        ), event, lender)
  }

  implicit val loanStatusFormat: Format[LoanStatus.Value] = Json.formatEnum(LoanStatus)
  implicit val loanStatusStatement: ToStatement[LoanStatus.Value] = (s: PreparedStatement, index: Int, v: LoanStatus.Value) =>
    s.setString(index, v match {
      case LoanStatus.AwaitingPickup => "AWAITING_PICKUP"
      case LoanStatus.AwaitingReturn => "AWAITING_RETURN"
      case LoanStatus.Returned => "RETURNED"
    })

  implicit def columnToLoanStatus: Column[LoanStatus.Value] =
    Column.columnToString.map {
      case "AWAITING_PICKUP" => LoanStatus.AwaitingPickup
      case "AWAITING_RETURN" => LoanStatus.AwaitingReturn
      case "RETURNED" => LoanStatus.Returned
    }

  implicit val objectStatusFormat: Format[ObjectStatus.Value] = Json.formatEnum(ObjectStatus)

  implicit def columnToObjectStatus: Column[ObjectStatus.Value] =
    Column.columnToString.map {
      case "IN_STOCK" => ObjectStatus.InStock
      case "OUT" => ObjectStatus.Out
      case "LOST" => ObjectStatus.Lost
      case "RESTING" => ObjectStatus.Resting
      case "DELETED" => ObjectStatus.Deleted

    }

  implicit val objectStatusStatement: ToStatement[ObjectStatus.Value] = (s: PreparedStatement, index: Int, v: ObjectStatus.Value) =>
    s.setString(index, v match {
      case ObjectStatus.InStock => "IN_STOCK"
      case ObjectStatus.Out => "OUT"
      case ObjectStatus.Lost => "LOST"
      case ObjectStatus.Resting => "RESTING"
      case ObjectStatus.Deleted => "DELETED"
    })

  implicit val datetimeRead: Reads[DateTime] = JodaReads.DefaultJodaDateTimeReads
  implicit val datetimeWrite: Writes[DateTime] = JodaWrites.JodaDateTimeWrites
  implicit val event: Format[Event] = Json.format[Event]
  implicit val lender: Format[ExternalLender] = Json.format[ExternalLender]
  implicit val loan: Format[ExternalLoan] = Json.format[ExternalLoan]
  implicit val completeLoan: Format[CompleteExternalLoan] = Json.format[CompleteExternalLoan]
  implicit val locationJson: Format[StorageLocation] = Json.format[StorageLocation]
  implicit val typeJson: Format[ObjectType] = Json.format[ObjectType]
  implicit val completeTypeJson: Format[CompleteObjectType] = Json.format[CompleteObjectType]
  implicit val obj: Format[SingleObject] = Json.format[SingleObject]
  implicit val complObj: Format[CompleteObject] = Json.format[CompleteObject]
  implicit val objLog: Format[ObjectLog] = Json.format[ObjectLog]
  implicit val complObjLog: Format[ObjectLogWithUser] = Json.format[ObjectLogWithUser]
  implicit val complObjLogWithObj: Format[ObjectLogWithObject] = Json.format[ObjectLogWithObject]
  implicit val objComment: Format[ObjectComment] = Json.format[ObjectComment]
  implicit val complObjComment: Format[CompleteObjectComment] = Json.format[CompleteObjectComment]

  implicit val objectTypeParser: RowParser[ObjectType] = Macro.namedParser[ObjectType](ColumnNaming.SnakeCase)

}
