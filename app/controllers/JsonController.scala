package controllers

import play.api.mvc._

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.db.slick._
import slick.driver.JdbcProfile
import models.Tables._
import javax.inject.Inject
import scala.concurrent.Future
import slick.driver.H2Driver.api._

import play.api.libs.json._
import play.api.libs.functional.syntax._

object JsonController {
    // ユーザ情報を受取るためのケースクラス
    case class UserForm(id: Option[Long], name: String, companyId: Option[Int])

    // UsersRowをJSONに変換するためのWritesを定義
    implicit val usersRowWrites = (
        (__ \ "id"       ).write[Long]   and
        (__ \ "name"     ).write[String] and
        (__ \ "companyId").writeNullable[Int]
    )(unlift(UsersRow.unapply))

    // JSONをUserFormに変換するためのReadsを定義
    implicit val userFormFormat = (
        (__ \ "id"       ).readNullable[Long] and
        (__ \ "name"     ).read[String]       and
        (__ \ "companyId").readNullable[Int]
    )(UserForm)
}

class JsonController @Inject()(val dbConfigProvider: DatabaseConfigProvider) extends Controller
    with HasDatabaseConfigProvider[JdbcProfile] {

    import JsonController._

    /**
     * 一覧表示
     */
    def list = Action.async { implicit rs =>
        // IDの昇順にすべてのユーザ情報を取得
        db.run(Users.sortBy(t => t.id).result).map { users =>
            // ユーザの一覧をJSONで返す
            Ok(Json.obj("users" -> users))
        }
    }

    /**
     * ユーザ登録
     */
    def create = Action.async(parse.json) { implicit rs =>
        rs.body.validate[UserForm].map { form =>
            // OKな場合はユーザを登録
            val user = UsersRow(0, form.name, form.companyId)
            db.run(Users += user).map { _ =>
                Ok(Json.obj("result" -> "success"))
            }
        }.recoverTotal { e =>
            // NGの場合はバリデーションエラーを返す
            Future {
                BadRequest(Json.obj("result" -> "failure", "error" -> JsError.toJson(e)))
            }
        }
    }

    /**
     * ユーザ更新
     */
    def update = Action.async(parse.json) { implicit rs =>
        rs.body.validate[UserForm].map { form =>
            // OKな場合はユーザ情報を更新
            val user = UsersRow(form.id.get, form.name, form.companyId)
            db.run(Users.filter(t => t.id === user.id.bind).update(user)).map { _ =>
                Ok(Json.obj("result" -> "success"))
            }
        }.recoverTotal { e =>
            // NGの場合はバリデーションエラーを返す
            Future {
                BadRequest(Json.obj("result" -> "failure", "error" -> JsError.toJson(e)))
            }
        }
    }

    /**
     * ユーザ削除
     */
    def remove(id: Long) = TODO

}