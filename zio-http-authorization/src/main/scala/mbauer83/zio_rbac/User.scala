package mbauer83.zio_http_authorization

import Permission._
import Role._
import Resource.{Resource, TenantId}

import zio.ZIO
import java.util.UUID
import zio.http.Request
import mbauer83.zio_http_authorization.Resource.ResourceSelector

/** Users are the basic subject of role-based authorization.
 *  They are generic in their type of [[User.UserId UserId]] and [[User.tenantId tenant id]].
 * 
 *  Aside from their identity, users contain their assigned [[Role.Role Roles]] and [[Permission.Permission Permissions]]
 *  which can be checked by [[AuthorizationPolicy.AuthorizationPolicy AuthorizationPolicies]].
 */
object User:
  /**
   * Users can be identified by a [[java.util.UUID]], [[scala.Symbol]],
   * [[String]], [[java.lang.Number]], or a [[scala.Product]] with a
   * [[scala.Serializable]].
   */
  type UserId = UUID | Symbol | String | Number | Product with Serializable

  /**
   * A user in the bounded context of role-based authorization.
   * 
   * `tenantId` is optional since superusers will not belong to any tenant.
   * User-models for single-tenant systems can use `Nothing` as the type for the tenantId.
   * 
   * @example Example usage without tenantId
   * {{{
   *   case class ExampleUser(id: String, roles: Set[Role], permissions: Set[(Permission, ResourceSelector[_, _])], otherData: String) extends User[String, Nothing]:
   *     val tenantId: Option[Nothing] = None
   * }}}
   * 
   * @example Example usage with UUID for id and tenantId
   * {{{
   *   case class ExampleUser(
   *     id: UUID, 
   *     tenantId: Option[UUID], 
   *     roles: Set[Role], 
   *     permissions: Set[(Permission, ResourceSelector[_, _])], 
   *     otherData: String
   *   ) extends User[UUID, UUID]
   * }}} 
   */
  trait User[I <: UserId, T <: TenantId] extends Product with Serializable:
    val id: I
    val tenantId: Option[T]
    val roles: Set[Role]
    val permissions: Set[(Permission, ResourceSelector[_, _])]

    /** Returns true if and only if the role is associated with the user.
      */
    def hasRole(role: Role): Boolean = roles.contains(role)
    /** Returns true if and only if the user has the permission
     *  with an associated [[Resource.ResourceSelector ResourceSelector]] that matches the given resource.
     */
    def hasPermissionForResource(permission: String | Symbol)(resource: Resource[?]): Boolean =
      permissions.filter(
        _._2.matches(resource)
      ).exists(
        _._1.name.equals(permission)
      )

  /**
    * Defines the DEFAULT user to be used when no user can be identified.
    */
  object User:
    val DEFAULT: User[Symbol, Nothing] = GenericUser[Symbol, Nothing](Symbol("DEFAULT"), None, Set.empty, Set.empty)

  /**
    * Simplifies [[User]]-models for single-tenant systems.
    */
  trait UserWithoutTenantId[I <: UserId] extends User[I, Nothing]:
    val tenantId: Option[Nothing] = None

  /**
   * An all-purpose user-model for multi-tenant systems.
   */
  case class GenericUser[I <: UserId, T <: TenantId](
    id: I,
    tenantId: Option[T],
    roles: Set[Role],
    permissions: Set[(Permission, ResourceSelector[_, _])],
  ) extends User[I, T]
