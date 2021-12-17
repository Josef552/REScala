This folder contains old code and experiments which is no longer maintained.

The Android support became too hard to maintain because of the reliance on Scala 2.11 and the general feeling that the Scala community lost interest in Android.

The meta project was an experiment too see what could be gained from not constructing the dataflow graph directly but just declare all reactives first, then do optimizations and analysis on a meta representation of the graph, and then build the graph from the resulting structure. However, this project essentially had to override all the existing APIs, which made it very brittle whenever the main implementation changed.

dividi and paroli are two case studies for an early version of CRDTs and delta CRDTs. Dividi is a graphical money sharing app, however it depends on some javafx extensions which are no longer maintained. Paroli is a console based chat using akka cluster which always breaks unexpectedly.
