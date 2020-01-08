package controllers

import java.time.Clock

import data.ExternalLoan
import javax.inject.Inject
import models.{EventsModel, LoansModel}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
class EventsController @Inject()(cc: ControllerComponents, model: EventsModel)(implicit ec: ExecutionContext, conf: Configuration, clock: Clock) extends AbstractController(cc) {
  def getCurrentEvent = Action.async { req =>
    model.getCurrentEvent().map(r => Ok(Json.toJson(r)))
  }
}
