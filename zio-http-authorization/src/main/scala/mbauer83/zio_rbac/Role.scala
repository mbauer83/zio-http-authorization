package mbauer83.zio_http_authorization

/** Roles are the basic units of role-based authorization.
 *  They are tags which can be associated in a many-to-many fashion with [[User.User Users]].
 *  [[AuthorizationPolicy.AuthorizationPolicy AuthorizationPolicies]] can then check if a given
 *  [[User.User User]] is authorized to to access or perform a specific kind of action on a given
 *  [[Resource.Resource Resource]] based on data from the resource and from the user, including 
 *  [[Role.Role Roles]] and [[Permission.Permission Permissions]].
 * 
 *  This library defined three common default roles: [[Role.SUPER SUPER]], [[Role.ADMIN ADMIN]], and [[Role.USER USER]].
 */
object Role:
  val SUPER: Role             = Role(Symbol("SUPER"))
  val ADMIN: Role             = Role(Symbol("ADMIN"))
  val USER: Role              = Role(Symbol("USER"))
  val defaultRoles: Set[Role] = Set[Role](SUPER, ADMIN, USER)

  /**
   * A specific role held by a [[User.User User]]
   */
  case class Role(name: String | Symbol)
