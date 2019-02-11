package swarm

import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLButtonElement, SVGElement}

import scala.scalajs.js.timers.SetIntervalHandle
import scala.scalajs.js.timers._
import scala.util.Random

import scala.math.abs

object Elements {
  import dom.document.{getElementById => resolve}

  val svgElement        = resolve("canvas").asInstanceOf[SVGElement]
  val btnStart          = resolve("start").asInstanceOf[HTMLButtonElement]
  val btnStop           = resolve("stop").asInstanceOf[HTMLButtonElement]
  val btnMoreBees       = resolve("more-bees").asInstanceOf[HTMLButtonElement]
  val btnHigherBeeAccel = resolve("higher-bee-accel").asInstanceOf[HTMLButtonElement]
}

object Main extends App {
  import Elements._

  val bbox = svgElement.getBoundingClientRect()
  val swarm = new Swarm(
    svgElement, bbox.width.toInt, bbox.height.toInt)

  var bees = Swarm.Bees
  var beeAccel = Swarm.BeeAcceleration

  btnStart.onclick = { e: Event =>
    swarm.animate(bees, beeAccel)
  }

  btnStop.onclick = { e: Event =>
    swarm.stop()
  }

  btnMoreBees.onclick = { e: Event =>
    swarm.stop()
    bees *= 10
    swarm.animate(bees, beeAccel)
  }

  btnHigherBeeAccel.onclick = { e: Event =>
    swarm.stop()
    beeAccel += 1
    swarm.animate(bees, beeAccel)
  }
}

object Swarm {
  val Bees             = 20   /* number of bees */
  val BeeAcceleration  = 3    /* acceleration of bees */
  val WaspAcceleration = 5    /* maximum acceleration of wasp */
  val BeeVelocity      = 11   /* maximum bee velocity */
  val WaspVelocity     = 12   /* maximum wasp velocity */
  val Delay            = 40   /* iteration every n milliseconds */
  val Border           = 100  /* wasp will not go closer than this to the edges */
}

case class Point(x: Int, y: Int)
case class Line(x1: Int, y1: Int, x2: Int, y2: Int)

/**
  * @param svgElement
  * @param width
  * @param height
  * @param waspVel  maximum wasp speed
  * @param beeVel   maximum bee speed
  * @param waspAcc  maximum wasp acceleration
  * @param delay    delay between updates, in milliseconds
  * @param border   border limiting wasp travel
  */
class Swarm(svgElement: SVGElement,
            width: Int,
            height: Int,
            waspVel: Int = Swarm.WaspVelocity,
            beeVel: Int = Swarm.BeeVelocity,
            waspAcc: Int = Swarm.WaspAcceleration,
            delay: Int = Swarm.Delay,
            border: Int = Swarm.Border) {
  val WaspId = 0
  val BeeId  = 1

  var interval: SetIntervalHandle = null

  def registerInterval(f: => Unit): Unit =
    interval = setInterval(delay) {
      dom.window.requestAnimationFrame { _ =>
        f
      }
    }

  def stop(): Unit = clearInterval(interval)

  /** Random positive integer */
  def randomInt(): Int =
    // Zero out the sign bit
    Random.nextInt() & Integer.MAX_VALUE

  /** Random velocity
    * @return Value around 0
    */
  def randomVel(v: Int): Int = (randomInt() % v) - (v / 2)

  def limitVel(current: Int, maximum: Int): Int =
    if (current > maximum) maximum
    else if (current < -maximum) -maximum
    else current

  /** Connects the points (x1, y1) and (x2, y2) */
  def drawLine(context: Int, line: Line, i: Int, colour: String): Unit = {
    var lineEl = dom.document.getElementById("line-" + context + "-" + i)

    if (lineEl == null) {
      lineEl = dom.document.createElementNS("http://www.w3.org/2000/svg", "line")
      svgElement.appendChild(lineEl)
    }

    import line._
    lineEl.setAttribute("id", "line-" + context + "-" + i)
    lineEl.setAttribute("x1", x1.toString)
    lineEl.setAttribute("y1", y1.toString)
    lineEl.setAttribute("x2", x2.toString)
    lineEl.setAttribute("y2", y2.toString)
    lineEl.setAttribute("stroke", colour)
  }

  def animate(bees: Int, beeAccel: Int): Unit = {
    /* wasp line and velocity */
    var waspLine = Line(
      border + randomInt() % (width - 2 * border),
      border + randomInt() % (height - 2 * border),
      0, 0)
    var waspVelocity = Point(0, 0)

    /* bee lines and velocities */
    var beeLines = Array.fill[Line](bees)(
      Line(
        randomInt() % width,
        randomInt() % height,
        0, 0))
    var beeVelocities = Array.fill[Point](bees)(Point(
      randomVel(7), randomVel(7)))

    registerInterval {
      /* accelerate wasp, limit velocity */
      waspVelocity = Point(
        limitVel(waspVelocity.x + randomVel(waspAcc), waspVel),
        limitVel(waspVelocity.y + randomVel(waspAcc), waspVel))

      /* move wasp */
      waspLine = Line(
        x1 = waspLine.x1 + waspVelocity.x,
        y1 = waspLine.y1 + waspVelocity.y,
        x2 = waspLine.x1,
        y2 = waspLine.y1)

      /* bounce check for x-coordinate */
      if (waspLine.x1 < border || waspLine.x1 > width - border - 1) {
        waspVelocity = waspVelocity.copy(x = -waspVelocity.x)
        waspLine = waspLine.copy(x1 = waspLine.x1 + (waspVelocity.x << 1))
      }

      /* bounce check for y-coordinate */
      if (waspLine.y1 < border || waspLine.y1 > height - border - 1) {
        waspVelocity = waspVelocity.copy(y = -waspVelocity.y)
        waspLine = waspLine.copy(y1 = waspLine.y1 + (waspVelocity.y << 1))
      }

      /* keep bees in motion */
      val randomBee = randomInt() % bees
      beeVelocities(randomBee) = Point(
        x = beeVelocities(randomBee).x + randomVel(3),
        y = beeVelocities(randomBee).y + randomVel(3))

      /* calculate new bee positions and velocities */
      val updated = beeLines.zip(beeVelocities).map { case (bee, velocity) =>
        /* calculate distance to wasp */
        val (dx, dy) = (
          waspLine.x1 - bee.x1,
          waspLine.y1 - bee.y1)

        /* approximate Euclidean distance, prevent division by zero */
        val distance = math.max(abs(dx) + abs(dy), 1)

        /* accelerate bee, limit velocity */
        val newVelocity = Point(
          limitVel(velocity.x + (dx * beeAccel) / distance, beeVel),
          limitVel(velocity.y + (dy * beeAccel) / distance, beeVel))

        /* move bee */
        val newBee = Line(
          x1 = bee.x1 + newVelocity.x,
          y1 = bee.y1 + newVelocity.y,
          x2 = bee.x1,
          y2 = bee.y1)

        (newBee, newVelocity)
      }.unzip
      beeLines      = updated._1
      beeVelocities = updated._2

      /* draw wasp and bees */
      drawLine(WaspId, waspLine, 0, "orange")
      beeLines.zipWithIndex.foreach { case (line, i) =>
        drawLine(BeeId, line, i, "white")
      }
    }
  }
}
