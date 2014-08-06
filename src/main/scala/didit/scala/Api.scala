package didit.scala

import net.minidev.json.JSONValue
import org.apache.http.client.methods.{HttpPost, HttpUriRequest, HttpGet}
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

// TODO Resolve default team from 'type' and more complex algorithm or ask user
class Api(val base:String, val token:String) {
  private val client = HttpClients.createDefault()

  def get(teamName:Option[String]=None):ScalaJSON = {
    val team = teamName.map(toTeam).getOrElse(getPersonalTeam).short_name.toString
    val req = new HttpGet(s"${base}dones/?team=${team}")
    execute(req).results
  }

  def post(raw:String, teamName:Option[String]=None) = {
    val team = teamName.map(toTeam).getOrElse(getPersonalTeam).url.toString
    val done =
      s"""{
         |"raw_text":"$raw",
         |"team":"$team"
          }""".stripMargin
    val req = new HttpPost(base+"dones/")
    req.setEntity(new StringEntity(done, ContentType.create("application/json", "UTF-8")))
    execute(req)
  }

  private def execute(req: HttpUriRequest) = {
    req.addHeader("Authorization", s"Token $token")
    req.addHeader("Content-Type", "application/json; charset=utf-8")
    req.addHeader("Accept", "application/json; charset=utf-8")

    val res = client.execute(req)
    val json = new ScalaJSON(JSONValue.parse(EntityUtils.toString(res.getEntity)))
    if ( res.getStatusLine.getStatusCode<400 ) {
      json
    } else {
      throw Error(res.getStatusLine.getStatusCode, res.getStatusLine.getReasonPhrase,
        json.detail.toString)
    }
  }

  private def toTeam(teamName:String)={
    val results = execute(new HttpGet(base+"teams/")).results
    results.toArray.find( _.short_name.toString==teamName).get
  }

  private def getPersonalTeam()= {
    val teams = execute(new HttpGet(s"${base}teams/")).results.toArray
    teams.find(_.is_personal.toBoolean).get
  }
}

case class Error(code:Int, status:String, detail:String) extends Throwable {
  override def toString = {
    s"${code}_${status} : $detail"
  }

  override def getMessage = detail
}

import net.minidev.json.JSONValue
import net.minidev.json.JSONArray
import net.minidev.json.JSONObject

import scala.language.dynamics

object JSON {
  def parseJSON(s: String) = new ScalaJSON(JSONValue.parse(s))

  implicit def ScalaJSONToString(s: ScalaJSON) = s.toString
  implicit def ScalaJSONToInt(s: ScalaJSON) = s.toInt
  implicit def ScalaJSONToDouble(s: ScalaJSON) = s.toDouble
}

class ScalaJSONIterator(i: java.util.Iterator[java.lang.Object]) extends Iterator[ScalaJSON] {
  def hasNext = i.hasNext()
  def next() = new ScalaJSON(i.next())
}

class ScalaJSON(o: java.lang.Object) extends Seq[ScalaJSON] with Dynamic {
  override def toString: String = o.toString
  def toInt: Int = o match {
    case i: Integer => i
    case _ => throw new Error(0, "Client error", "Cannot convert "+o+" to int")
  }
  def toDouble: Double = o match {
    case d: java.lang.Double => d
    case f: java.lang.Float => f.toDouble
    case _ => throw new Error(0, "Client error", "Cannot convert "+o+" to double")
  }
  def toBoolean: Boolean = {
    o.toString.toBoolean
  }
  def apply(key: String): ScalaJSON = o match {
    case m: JSONObject => new ScalaJSON(m.get(key))
    case _ => throw new Error(0, "Client error", "Cannot parse "+o)
  }

  def apply(idx: Int): ScalaJSON = o match {
    case a: JSONArray => new ScalaJSON(a.get(idx))
    case _ => throw new Error(0, "Client error", "Cannot parse "+o)
  }
  def length: Int = o match {
    case a: JSONArray => a.size()
    case m: JSONObject => m.size()
    case _ => throw new Error(0, "Client error", "Cannot get length of "+o)
  }
  def iterator: Iterator[ScalaJSON] = o match {
    case a: JSONArray => new ScalaJSONIterator(a.iterator())
    case _ => throw new Error(0, "Client error", "Cannot iterate on "+o)
  }

  def selectDynamic(name: String): ScalaJSON = apply(name)
  def applyDynamic(name: String)(arg: Any) = {
    arg match {
      case s: String => apply(name)(s)
      case n: Int => apply(name)(n)
      case u: Unit => apply(name)
    }
  }
}

