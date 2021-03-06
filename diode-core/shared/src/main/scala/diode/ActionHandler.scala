package diode

import diode.util.RunAfter

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

/**
  * Base class for all action handlers.
  *
  * @param modelRW Model reader/writer for the actions this handler processes.
  */
abstract class ActionHandler[M, T](val modelRW: ModelRW[M, T]) {

  import ActionResult._

  private var currentModel: M = modelRW.root.value

  lazy val liftedHandler = handle.lift

  /**
    * Handles the incoming action by updating current model and calling the real `handle` function
    */
  def handleAction(model: M, action: AnyRef): Option[ActionResult[M]] = {
    currentModel = model
    liftedHandler(action)
  }

  /**
    * Override this function to handle dispatched actions.
    */
  protected def handle: PartialFunction[AnyRef, ActionResult[M]]

  /**
    * Helper function that returns the current value from the model.
    */
  def value: T = modelRW.eval(currentModel)

  /**
    * Helper function to create a `ModelUpdate` result from a new value.
    *
    * @param newValue
    * @return
    */
  def updated(newValue: T): ActionResult[M] =
    ModelUpdate(modelRW.updatedWith(currentModel, newValue))

  /**
    * Helper function to create a `ModelUpdateEffect` result from a new value and an effect.
    *
    * @param newValue
    * @param effect
    * @return
    */
  def updated(newValue: T, effect: Effect): ActionResult[M] =
    ModelUpdateEffect(modelRW.updatedWith(currentModel, newValue), effect)

  /**
    * Helper function when the action does no model changes or effects.
    *
    * @return
    */
  def noChange: ActionResult[M] =
    NoChange

  /**
    * Helper function to create an `EffectOnly` result with the provided effect.
    *
    * @param effect
    * @return
    */
  def effectOnly(effect: Effect): ActionResult[M] =
    EffectOnly(effect)

  /**
    * Helper function to create a delayed effect.
    *
    * @param delay How much to delay the effect.
    * @param f     Result of the effect
    */
  def runAfter[A <: AnyRef](delay: FiniteDuration)(f: => A)(implicit runner: RunAfter, ec: ExecutionContext): Effect =
    Effect(runner.runAfter(delay)(f))
}

object ActionHandler {
  implicit def extractHandler[M <: AnyRef](actionHandler: ActionHandler[M, _]): (M, AnyRef) => Option[ActionResult[M]] =
    actionHandler.handleAction
}
