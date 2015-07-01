package models.daos

import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import models.User
import models.daos.UserDAOStub._

import scala.collection.mutable
import scala.concurrent.Future

/**
 * Give access to the user object.
 */
class UserDAOStub extends UserDAO {

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo) = Future.successful(
    users.find { case (id, user) => user.loginInfo == loginInfo }.map(_._2)
  )

  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userID: Long) = Future.successful(users.get(userID))

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User) = {
    users += (user.userID.getOrElse(if (users.keySet.isEmpty) 1 else users.keySet.max+1) -> user)
    Future.successful(user)
  }
}

/**
 * The companion object.
 */
object UserDAOStub {

  /**
   * The list of users.
   */
  val users: mutable.HashMap[Long, User] = mutable.HashMap()
}
