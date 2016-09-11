package com.github.matthewpflueger.isomorphic.server

import java.io.InputStreamReader
import javax.script.ScriptEngineManager

import akka.http.scaladsl.model.{ContentType, HttpCharsets, HttpEntity, MediaTypes}
import jdk.nashorn.api.scripting.NashornScriptEngine
import com.eclipsesource.v8
import com.eclipsesource.v8.V8

import scala.io.Source

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
  
  def mkString(path: String) = 
      Source.fromInputStream(HelloWorld.getClass.getClassLoader.getResourceAsStream(path)).mkString

  def render(name: String) = {
    // this is here only because the V8 runtime can only be accessed by a single thread
    // this should be moved into an actor...
    
    val v8 = V8.createV8Runtime("global")
    v8.executeVoidScript(mkString("web/js/isomorphic-jsdeps.js"))
    v8.executeVoidScript(mkString("web/js/isomorphic-fastopt.js"))
    val content = v8.executeStringScript(s"""HelloWorld().renderServer("$name")""")
    
    
//    val hwf = nashorn.invokeFunction("HelloWorld")
//    val content = nashorn.invokeMethod(hwf, "renderServer", name)

    val he = HttpEntity(
      ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`),
      hw.render(name, String.valueOf(content)))
    v8.release()
    he
  }

  val hw = HelloWorld(scalatags.Text)
  
//  val nashorn = new ScriptEngineManager().getEngineByName("nashorn").asInstanceOf[NashornScriptEngine]
//
//  nashorn.eval("var global = this;")
//  nashorn.eval(read("web/js/isomorphic-jsdeps.js"))
//  nashorn.eval(read("web/js/isomorphic-fastopt.js"))
}
