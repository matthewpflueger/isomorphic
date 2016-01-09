package com.github.matthewpflueger.isomorphic.server


import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import java.io.InputStreamReader
import javax.script.ScriptEngineManager

import akka.http.scaladsl.model.{HttpCharsets, MediaTypes, ContentType, HttpEntity}
import jdk.nashorn.api.scripting.NashornScriptEngine


object Main {

  def main(args: Array[String]): Unit = {
    println("Starting")

    implicit val system = ActorSystem("Isomorphic")
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher

    val public =
      get {
        getFromResourceDirectory("web") ~
        path("api" / "hello") {
          complete {
            HelloWorld.render("World")
          }
        }
      }


    val route = decodeRequest { encodeResponse { public } }

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)

    bindingFuture.onComplete(_ => println("Started"))
    Await.result(bindingFuture, Duration.Inf)
  }

}
