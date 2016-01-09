package com.github.matthewpflueger.isomorphic.client

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom

@JSExport("HelloWorld")
object HelloWorld {

  class Backend($: BackendScope[String, Unit]) {
    def render(s: String) = <.div(s"Hello $s")
  }

  val Hello = ReactComponentB[String]("HelloWorld")
    .renderBackend[Backend]
    .build

  @JSExport
  def renderServer(name: String): String = {
    ReactDOMServer.renderToString(Hello(name))
  }

  @JSExport
  def renderClient(name: String, elementId: String): Unit = {
    ReactDOM.render(Hello(name), dom.document.getElementById(elementId))
  }
}
