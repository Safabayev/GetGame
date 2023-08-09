import sbt.*

object Dependencies {
  object Versions {
    lazy val circe = "0.14.3"
    lazy val http4s = "0.23.10"
    lazy val flyway = "9.16.0"
    lazy val cats = "2.9.0"
    lazy val `cats-effect` = "3.4.8"
    lazy val logback = "1.4.7"
    lazy val log4cats = "2.5.0"
    lazy val pureconfig = "0.17.2"
    lazy val enumeratum = "1.7.0"
    lazy val postgresql = "42.5.4"
    lazy val sangria = "3.5.3"
    lazy val `sangria-circe` = "1.3.2"
    lazy val sttp = "3.7.2"
  }
  trait LibGroup {
    def all: Seq[ModuleID]
  }
  object com {
    object github {
      object pureconfig extends LibGroup {
        private def repo(artifact: String): ModuleID =
          "com.github.pureconfig" %% artifact % Versions.pureconfig

        lazy val core: ModuleID = repo("pureconfig")
        lazy val enumeratum: ModuleID = repo("pureconfig-enumeratum")

        override def all: Seq[ModuleID] = Seq(core, enumeratum)
      }
    }

    object softwaremill {
      object sttp extends LibGroup {
        private def sttp(artifact: String): ModuleID =
          "com.softwaremill.sttp.client3" %% artifact % Versions.sttp

        lazy val circe: ModuleID = sttp("circe")
        lazy val `fs2-backend`: ModuleID = sttp("async-http-client-backend-fs2")
        override def all: Seq[ModuleID] = Seq(circe, `fs2-backend`)
      }
    }

  }
  object io {
    object getquill extends LibGroup {
      lazy val doobie = "io.getquill" %% "quill-doobie" % "4.6.0"
      lazy val jdbc = "io.getquill"   %% "quill-jdbc"   % "4.6.1"
      override def all: Seq[ModuleID] = Seq(doobie, jdbc)
    }
    object circe extends LibGroup {
      private def circe(artifact: String): ModuleID =
        "io.circe" %% s"circe-$artifact" % Versions.circe

      lazy val core: ModuleID = circe("core")
      lazy val generic: ModuleID = circe("generic")
      lazy val parser: ModuleID = circe("parser")
      lazy val `generic-extras`: ModuleID = circe("generic-extras")
      override def all: Seq[ModuleID] = Seq(core, generic, parser, `generic-extras`)
    }
  }
  object org {
    lazy val postgresql: ModuleID = "org.postgresql" % "postgresql" % Versions.postgresql
    object typelevel {
      object cats {
        lazy val core = "org.typelevel"           %% "cats-core"           % Versions.cats
        lazy val effect = "org.typelevel"         %% "cats-effect"         % Versions.`cats-effect`
      }
    }
    object http4s extends LibGroup {
      private def http4s(artifact: String): ModuleID =
        "org.http4s" %% s"http4s-$artifact" % Versions.http4s

      lazy val dsl = http4s("dsl")
      lazy val server = http4s("ember-server")
      lazy val client = http4s("ember-client")
      lazy val circe = http4s("circe")
      lazy val `blaze-server` = http4s("blaze-server")
      override def all: Seq[ModuleID] = Seq(dsl, server, client, circe)
    }
    object sangria extends LibGroup {
      lazy val core = "org.sangria-graphql"  %% "sangria"       % Versions.sangria
      lazy val circe = "org.sangria-graphql" %% "sangria-circe" % Versions.`sangria-circe`
      override def all: Seq[ModuleID] = Seq(core, circe)
    }
    object flywaydb {
      lazy val core = "org.flywaydb" % "flyway-core" % Versions.flyway
    }
  }

  object ch {
    object qos {
      lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.logback
    }
  }

}
