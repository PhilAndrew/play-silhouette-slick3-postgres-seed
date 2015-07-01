package models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import play.api.libs.concurrent.Execution.Implicits._
import slick.driver.JdbcProfile
import scala.collection.mutable
import scala.concurrent.Future

/**
 * The DAO to store the password information.
 */
class PasswordInfoDAOImpl extends DelegableAuthInfoDAO[PasswordInfo]  with HasDatabaseConfig[JdbcProfile] {
  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  import driver.api._
  import models.daos.UserDAOImpl._
  import models.daos.PasswordInfoDAOImpl._

  /**
   * Finds the auth info which is linked with the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The retrieved auth info or None if no auth info could be retrieved for the given login info.
   */
  def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = {
    {
      db.run(LoginInfos.filter(i => i.providerKey === loginInfo.providerKey && i.providerID === loginInfo.providerID)
        .result.headOption).mapTo[Option[LoginInfoRow]].flatMap {
        case Some(loginInfoRow) => db.run(PasswordInfos.filter(_.loginInfoId === loginInfoRow.id).result.headOption).mapTo[Option[PasswordInfoRow]].flatMap {
          case Some(passrow) => Future.successful(Some(PasswordInfo(passrow.hasher, passrow.password, passrow.salt)))
          case None => Future.successful(None)
        }
        case None => Future.successful(None)
      }
    }
  }

  /**
   * Adds new auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be added.
   * @param authInfo The auth info to add.
   * @return The added auth info.
   */
  def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    db.run(LoginInfos.filter(i => i.providerKey === loginInfo.providerKey && i.providerID === loginInfo.providerID)
      .result.headOption).mapTo[Option[LoginInfoRow]].flatMap {
      case Some(loginInfoRow) => db.run(PasswordInfos += PasswordInfoRow(authInfo.hasher, authInfo.password, authInfo.salt, loginInfoRow.id.get)).flatMap {
        _ => Future.successful(authInfo)
      }
      case None =>
        //TODO LOG ERROR
        Future.successful(authInfo)
    }
  }

  /**
   * Updates the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be updated.
   * @param authInfo The auth info to update.
   * @return The updated auth info.
   */
  def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    db.run(LoginInfos.filter(i => i.providerKey === loginInfo.providerKey && i.providerID === loginInfo.providerID)
      .result.headOption).mapTo[Option[LoginInfoRow]].flatMap {
      case Some(loginInfoRow) => db.run(PasswordInfos.filter(_.loginInfoId === loginInfoRow.id.get).update(PasswordInfoRow(authInfo.hasher, authInfo.password, authInfo.salt, loginInfoRow.id.get))).flatMap {
        _ => Future.successful(authInfo)
      }
      case None =>
        Future.failed(new Exception("Can't update authInfo"))
    }
  }

  /**
   * Saves the auth info for the given login info.
   *
   * This method either adds the auth info if it doesn't exists or it updates the auth info
   * if it already exists.
   *
   * @param loginInfo The login info for which the auth info should be saved.
   * @param authInfo The auth info to save.
   * @return The saved auth info.
   */
  def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    find(loginInfo).flatMap {
      case Some(_) => update(loginInfo, authInfo)
      case None => add(loginInfo, authInfo)
    }
  }

  /**
   * Removes the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be removed.
   * @return A future to wait for the process to be completed.
   */
  def remove(loginInfo: LoginInfo): Future[Unit] = {
    Future.failed(new Exception("Can't delete authInfo // TODO"))
  }

}

/**
 * The companion object.
 */
object PasswordInfoDAOImpl extends HasDatabaseConfig[JdbcProfile] {

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  import driver.api._

  class PasswordInfosTable(tag: Tag) extends Table[PasswordInfoRow](tag, "users_passwordinfo") {
    def hasher = column[String]("hasher")
    def password = column[String]("password")
    def salt = column[Option[String]]("salt")
    def loginInfoId = column[Long]("logininfo_id", O.PrimaryKey)
    def * = (hasher, password, salt, loginInfoId) <> (PasswordInfoRow.tupled,PasswordInfoRow.unapply _)
  }
  case class PasswordInfoRow(hasher : String, password : String,salt :  Option[String], loginInfoId : Long)

  val PasswordInfos = TableQuery[PasswordInfosTable]
}
