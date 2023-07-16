package mbauer83.zio_http_authorization

import Resource._

/** Permissions define specific (kinds of) actions that may be performed on a [[Resource.Resource Resource]]
 *  by a [[User.User User]] to which the permission is assigned together with a [[Resource.ResourceSelector ResourceSelector]].
 * 
 *  Permissions contain a name which may be of type [[String]] or [[Symbol]].
 */
object Permission:

  /**
   * A specific permission held by a [[User.User User]].
   */
  case class Permission(name: String | Symbol)