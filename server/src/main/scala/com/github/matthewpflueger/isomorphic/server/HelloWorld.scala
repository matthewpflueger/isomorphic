package com.github.matthewpflueger.isomorphic.server

import java.io.InputStreamReader
import javax.script.ScriptEngineManager

import akka.http.scaladsl.model.{HttpCharsets, MediaTypes, ContentType, HttpEntity}
import jdk.nashorn.api.scripting.NashornScriptEngine

case class HelloWorld[Builder, Output <: FragT, FragT]
  (bundle: scalatags.generic.Bundle[Builder, Output, FragT]) {

  import bundle.all._

  def render(name: String, content: String) =
    "<!DOCTYPE html>" + html(
      head(
        script(src := "/js/isomorphic-jsdeps.js"),
        script(src := "/js/isomorphic-fastopt.js")
      ),
      body(
        h1("Isomorphic"),
        div(id := "content", raw(content)),
        script(raw(s"""HelloWorld().renderClient("$name", "content")"""))
      )
    )
}

object HelloWorld {

  def read(path: String) = {
    new InputStreamReader(HelloWorld.getClass.getClassLoader.getResourceAsStream(path))
  }

  def render(name: String) = {
    val hwf = nashorn.invokeFunction("HelloWorld")
    val content = nashorn.invokeMethod(hwf, "renderServer", name)

    HttpEntity(
      ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`),
      hw.render(name, String.valueOf(content)))
  }

  val hw = HelloWorld(scalatags.Text)
  val nashorn = new ScriptEngineManager().getEngineByName("nashorn").asInstanceOf[NashornScriptEngine]

  nashorn.eval("var global = this;")
  nashorn.eval(read("web/js/isomorphic-jsdeps.js"))
  nashorn.eval(read("web/js/isomorphic-fastopt.js"))
}
