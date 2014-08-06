package didit.scala

import java.io.{FileInputStream, FileWriter, RandomAccessFile, File}
import java.lang.reflect.{Modifier, InvocationTargetException, Method}
import java.util.Properties

object Didit extends App {
  val api = new Api("https://idonethis.com/api/v0.0/", token)
  private var selectedTeam = None
  try {
    if ( args.length>0 ) {
      val arguments = args.drop(1)
      getClass.getMethod(args(0), arguments.getClass)
        .invoke(this, arguments)
    } else {
      help(Array(""))
    }
  } catch {
    case e:NoSuchMethodException => {
      println(s"didit: Unknown command ${args(0)}. Try `help` for available commands")
    }
    case e:InvocationTargetException => {
      e.printStackTrace()
      val cause = e.getCause;
      println(s"didit: '${args(0)}' failed due to ${cause.getMessage}.")
    }
  }

  @Doc(text="Show usage and quit.")
  def help(args:Array[String]) {
    println(Help(this))
  }

  @Doc(text="Display your 10 latest dones ")
  def list(args:Array[String]) {
    val fromTeam = if ( args.length>1 && args(0)=="from" ) {
      Some(args(1))
    } else {
      None
    }
    for (done <- (api get fromTeam ).toArray) {
      println(done.raw_text)
    }
  }

  @Doc(text=
    "Add a done for today. The team must be identified after the keyword 'in'. When the team isn't identified, " +
    "the personal one is used.\n"+
    "In case your done contains 'in', it must be wrapped between double quotes.\n" +
    " $ add in team-name \"Living in brussel\"")
  def add(args:Array[String]) {
    if ( args.length<1 ) {
      println("didit: Invalid arguments. A done is required")
    } else if ( args(0)=="in" ) {
      val team = args(1)
      val text = args.drop(2).mkString(" ")
      val res = api post(text, Some(team))
      println(s"Ok (#${res.id.toString})")
    } else {
      val text = args.mkString(" ").trim
      val res = api post(text, None)
      println(s"Ok (#${res.id.toString})")
    }
  }

  private def token() = {
    val cfg  = new Properties()
    val file = new File(System.getProperty("user.home")+"/.didit/config.cfg")
    if ( !file.exists() ) {
      file.getParentFile.mkdirs()
      println("Please enter your api token.")
      println("  your token is available on https://idonethis.com/api/v0.0/get_token/")
      new FileWriter(file)
        .append("api.token = ").append(readLine()).close()
    }
    val reader = new FileInputStream(file)
    try {
      cfg.load(reader)
      cfg.getProperty("api.token")
    } finally  {
      reader.close()
    }
  }

}

private case class Help(app:App) {
  private def reservedNames = Seq("main", "args", "delayedInit", "executionStart")

  override def toString() = {
    val commands = for (m <- app.getClass.getDeclaredMethods if isCommand(m)) yield description(m)
    """Client for iDoneThis api (https://idonethis.com/).
      |
      |Available commands :
    """.stripMargin +
    commands.mkString("\n\t")
  }

  private def description(m:Method) = {
    val doc = Option(m.getAnnotation(classOf[Doc]))
    m.getName + doc.map("\n\t    "+_.text()).getOrElse("")
  }

  private def isCommand(m:Method)=
    !m.getName.contains("$") &&
    !reservedNames.contains(m.getName) &&
    Modifier.isPublic(m.getModifiers)
}


