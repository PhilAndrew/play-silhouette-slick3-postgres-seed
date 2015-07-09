package models.daos


import com.mohiva.play.silhouette.api.LoginInfo
import models.User
import play.api.Play
import play.api.db.slick.{HasDatabaseConfig, DatabaseConfigProvider}
import slick.driver.JdbcProfile
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfig
import slick.driver.JdbcProfile
import scala.collection.mutable
import scala.util.Failure
import models.daos.UserDAOImpl.{LoginInfoRow, UserRow}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Give access to the user object.
 */
class UserDAOImpl extends UserDAO with HasDatabaseConfig[JdbcProfile] {
  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  import driver.api._

  import models.daos.UserDAOImpl._

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo) = {  db.run(LoginInfos.filter(i => i.providerKey === loginInfo.providerKey && i.providerID === loginInfo.providerID)
    .result.headOption).mapTo[Option[LoginInfoRow]].flatMap{
    case Some(loginInfoRow) => db.run(UserLoginInfos.filter(_.loginInfoId === loginInfoRow.id).result.headOption).mapTo[Option[UserLoginInfoRow]].flatMap{
        case Some(userLoginInfoRow) =>  find(userLoginInfoRow.userID)
        case None => Future.successful(None)
      }
    case None =>   Future.successful(None)
    }
  }

  /**
   * Finds a login info for a user.
   *
   * @param userID The userId.
   * @return The found loginInfo.
   */
  def findLoginInfo(userID: Long) : Future[Option[LoginInfo]] = {
    db.run(UserLoginInfos.filter(_.userID === userID).result.headOption).mapTo[Option[UserLoginInfoRow]].flatMap{
      case Some(userLoginInfoRow) =>
        db.run(LoginInfos.filter(_.id === userLoginInfoRow.loginInfoId).result.headOption).mapTo[Option[LoginInfoRow]].flatMap{
          case Some(loginInfo) =>  Future.successful(Some(LoginInfo(loginInfo.providerId,loginInfo.providerKey)))
          case None => Future.successful(None)
      }
      case None => Future.successful(None)
    }
  }


  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userID: Long) = {
    db.run(Users.filter(_.id === userID).result.headOption).mapTo[Option[UserRow]].flatMap{
      case Some(user) => findLoginInfo(user.userID.get).mapTo[Option[LoginInfo]].flatMap {
        case Some(loginInfo) => Future.successful(Some(UserRowToUser(user,loginInfo)))
        case None => Future.successful(None)
      }
      case None =>   Future.successful(None)
    }
  }

  /**
   * Finds a user by its user email.
   *
   * @param email The email of the user to find.
   * @return The found user or None if no user for the given email could be found.
   */
  def find(email: String) = {
    db.run(Users.filter(_.email === email).result.headOption).mapTo[Option[UserRow]].flatMap{
      case Some(user) => findLoginInfo(user.userID.get).mapTo[Option[LoginInfo]].flatMap {
        case Some(loginInfo) => Future.successful(Some(UserRowToUser(user,loginInfo)))
        case None => Future.successful(None)
      }
      case None =>   Future.successful(None)
    }
  }


  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User) =
    user.userID match {
      case Some(id) => db.run(Users.filter(_.id === id).update(UserToUserRow(user))).mapTo[Integer].flatMap {
        updated =>
          if (updated > 0) {
            save(user.loginInfo,id).flatMap{ _ => Future.successful(user)}
          } else {
            create(user)
          }
      }
      case None => create(user)
    }


  /**
   * Saves a loginInfo. If the user iD is undefined, creates a new user.
   *
   * @param loginInfo The loginInfo to save.
   * @return The saved loginInfo.
   */
  def save(loginInfo: LoginInfo,userId : Long) : Future[LoginInfo] = {
    db.run(LoginInfos.filter(i => i.providerKey === loginInfo.providerKey && i.providerID === loginInfo.providerID).result.headOption).mapTo[Option[LoginInfoRow]].flatMap {
      case Some(loginInfoRow) => Future.successful(loginInfo)
      case None =>
        db.run((LoginInfos returning LoginInfos.map(_.id)) += LoginInfoRow(None,loginInfo.providerID,loginInfo.providerKey)).mapTo[Long].flatMap{
          id => db.run(UserLoginInfos += UserLoginInfoRow(userId,id)).flatMap{ _ => Future.successful(loginInfo)}
        }
      }
  }

  /**
   * Creates a user.
   *
   * @param user The user to Create.
   * @return The created user.
   */
  def create(user: User): Future[User] = {
    db.run((Users returning Users.map(_.id)) += UserToUserRow(user)).mapTo[Long].flatMap {
      id => save(user.loginInfo, id).flatMap {
        loginInfo => Future.successful(User(Some(id), loginInfo, user.firstName, user.lastName, user.email, user.avatarURL))
      }
    }
  }

}

/**
 * The companion object.
 */
object UserDAOImpl  extends HasDatabaseConfig[JdbcProfile] {

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  import driver.api._

  case class UserRow(userID: Option[Long],firstName: String,lastName: String,email: String,avatarURL: Option[String])
  case class LoginInfoRow(id : Option[Long], providerId : String,providerKey : String)
  case class UserLoginInfoRow(userID : Long, loginInfoId : Long)

  val Users = TableQuery[UsersTable]
  val LoginInfos = TableQuery[LoginInfosTable]
  val UserLoginInfos= TableQuery[UserLoginInfosTable]


  def UserRowToUser(row : UserRow, loginInfo : LoginInfo) : User = {
    User(row.userID,loginInfo,Some(row.firstName),Some(row.lastName),Some(row.email),row.avatarURL)
  }

  def UserToUserRow(row : User) : UserRow = {
    UserRow(row.userID,row.firstName.getOrElse(""),row.lastName.getOrElse(""),row.email.getOrElse(""),row.avatarURL)
  }


  class UsersTable(tag: Tag) extends Table[UserRow](tag, "users") {
    def id = column[Long]("user_id", O.PrimaryKey, O.AutoInc)
    def firstName = column[String]("first_name")
    def lastName = column[String]("last_name")
    def email = column[String]("email")
    def avatarURL = column[Option[String]]("avatar_url")
    def uniqueEmail = index("IDX_EMAIL", email, unique = true)
    def * = (id.?, firstName, lastName, email, avatarURL) <> (UserRow.tupled,UserRow.unapply _)
  }

  class LoginInfosTable(tag: Tag) extends Table[LoginInfoRow](tag, "users_logininfo") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def providerID = column[String]("provider_id")
    def providerKey = column[String]("provider_key")
    def * = (id.?, providerID, providerKey) <> (LoginInfoRow.tupled,LoginInfoRow.unapply _ )
  }

  class UserLoginInfosTable(tag: Tag) extends Table[UserLoginInfoRow](tag, "users_userlogininfo") {
    def userID = column[Long]("user_id")
    def loginInfoId = column[Long]("logininfo_id")
    def * = (userID, loginInfoId) <> (UserLoginInfoRow.tupled,UserLoginInfoRow.unapply _)
  }

}
