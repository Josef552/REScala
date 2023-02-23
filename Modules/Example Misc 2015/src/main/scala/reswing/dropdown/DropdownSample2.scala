package reswing.dropdown

import rescala.default._
import reswing.ReTextField._
import reswing._

import scala.swing.{BoxPanel, FlowPanel, Label, MainFrame, Orientation, SimpleSwingApplication}

object DropdownSample2 extends SimpleSwingApplication {

  def top =
    new MainFrame {

      /* This version "artificially" introduces a Signal[List[Signal[String]] to illustrate higher
       * order signals, but this is not really necessary (See DropdownSample1)
       */

      val col1 = new ReTextField(text = "Berlin", columns = 30)
      val col2 = new ReTextField(text = "Paris", columns = 30)
      val col3 = new ReTextField(text = "London", columns = 30)
      val col4 = new ReTextField(text = "Rome", columns = 30)

      val val1 = Signal { col1.text() }
      val val2 = Signal { col2.text() }
      val val3 = Signal { col3.text() }
      val val4 = Signal { col4.text() }

      val listOfSignals = Signal { List(val1, val2, val3, val4) }
      val options       = Signal.dynamic { listOfSignals().map(_()) }

      val dropdown       = new ReDynamicComboBox(options = options, selection = -1)
      val selectionIndex = Signal { dropdown.selection() }
      val validSelection = Signal { if (options().indices.contains(selectionIndex())) Some(selectionIndex()) else None }

      // select the currently selected item manually
      val currentSelectedItem = Signal.dynamic { validSelection().map { i => listOfSignals()(i)() } }
      val outputString        = Signal { currentSelectedItem().getOrElse("Nothing") }
      val outputField         = new ReTextField(text = outputString)

      title = "Dropdown example 2"
      contents = new BoxPanel(Orientation.Vertical) {

        contents += new FlowPanel {
          contents += new Label("Value 1:")
          contents += col1
        }

        contents += new FlowPanel {
          contents += new Label("Value 2:")
          contents += col2
        }

        contents += new FlowPanel {
          contents += new Label("Value 3:")
          contents += col3
        }

        contents += new FlowPanel {
          contents += new Label("Value 4:")
          contents += col4
        }

        contents += new FlowPanel {
          contents += new Label("Dropdown selection: ")
          contents += dropdown
        }

        contents += new FlowPanel {
          contents += new Label("Selected item: ")
          contents += outputField
        }
      }
    }
}