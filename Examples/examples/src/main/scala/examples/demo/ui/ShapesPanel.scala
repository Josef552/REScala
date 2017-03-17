package examples.demo.ui;

import java.awt.{Color, Graphics2D, RenderingHints}

import rescala._

import scala.swing.Panel

class ShapesPanel(initShapes: Shape*) extends Panel {
  val shapes = Var(initShapes.toList)
  //val allChanges: Event[Any] = Event { shapes().find{ shape: Shape => shape.changed().isDefined } }
  val allChanges: Event[Any] = shapes.map(_.map(_.changed)).flatten
  allChanges observe {_ => repaint() }

  override def paintComponent(g: Graphics2D): Unit = {
    implicitEngine.plan() {implicit turn =>
      g.setColor(Color.WHITE)
      g.fillRect(0, 0, size.width, size.height)
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g.translate(size.width / 2, size.height / 2)
      for (shape <- shapes.now) {
        shape.drawSnapshot(g)
      }
    }
  }
}

trait Shape {
  val changed: Event[Any]
  def centerX: Signal[Int]
  def centerY: Signal[Int]
  def hitboxWidth: Signal[Int]
  def hitboxHeight: Signal[Int]
  def drawSnapshot(g: Graphics2D)(implicit turn: Turn): Unit
}

class Circle (override val centerX: Signal[Int],
              override val centerY: Signal[Int],
              val diameter: Signal[Int],
              val border: Signal[Option[Color]] = Var(Some(Color.BLACK)),
              val fill: Signal[Option[Color]] = Var(None)) extends Shape {
  override val changed = centerX.changed || centerY.changed || diameter.changed || border.changed || fill.changed
  override val hitboxWidth = diameter
  override val hitboxHeight = diameter
  override def drawSnapshot(g: Graphics2D)(implicit turn: Turn): Unit = {
    val d = diameter.now
    val x = centerX.now - d/2
    val y = centerY.now - d/2
    val f = fill.now
    if(f.isDefined) {
      g.setColor(f.get)
      g.fillOval(x, y, d, d)
    }
    val b = border.now
    if(b.isDefined) {
      g.setColor(b.get)
      g.drawOval(x, y, d, d)
    }
  }
}

class Rectangle (override val centerX: Signal[Int],
                 override val centerY: Signal[Int],
                 override val hitboxWidth: Signal[Int],
                 override val hitboxHeight: Signal[Int],
                 val border: Signal[Option[Color]] = Var(Some(Color.BLACK)),
                 val fill: Signal[Option[Color]] = Var(None)) extends Shape {
  override val changed = centerX.changed || centerY.changed || hitboxWidth.changed || hitboxHeight.changed || border.changed || fill.changed
  override def drawSnapshot(g: Graphics2D)(implicit turn: Turn): Unit = {
    val w = hitboxWidth.now
    val h = hitboxHeight.now
    val x = centerX.now - w/2
    val y = centerY.now - h/2
    val f = fill.now
    if(f.isDefined) {
      g.setColor(f.get)
      g.fillRect(x, y, w, h)
    }
    val b = border.now
    if(b.isDefined) {
      g.setColor(b.get)
      g.drawRect(x, y, w, h)
    }
  }
}
