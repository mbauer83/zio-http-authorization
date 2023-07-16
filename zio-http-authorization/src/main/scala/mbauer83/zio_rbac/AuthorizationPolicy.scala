package mbauer83.zio_http_authorization

import Resource._
import User._

import zio.ZIO
import scala.reflect.ClassTag
import mbauer83.zio_http_authorization.AuthorizationPolicy.UserNotAuthorizedForResourceException
import zio.http.Request
import zio.ZIOAppDefault

/**
  * AuthorizationPolicies hold information about the type of [[User.User User]] and [[Resource.Resource Resource]]
  * they authorize, and define an effect to authorize a user of the specified type to access a resource of the
  * specified type.
  * 
  * Out of the box, this library provides a policy which always denies access,
  * a policy which always grants access, and a policy which grants access if the user has the [[Role.SUPER SUPER Role]],
  * or if the resource's tenant-id matches the user's tenant-id, or if the resource has no tenant-id.
  */
object AuthorizationPolicy:
  /**
   *
   * Defines a policy for authorizing a specific type `U` of [[User.User]] to access
   * a specific type `R` of [[Resource.Resource]].
   *
   * Reflection using [[scala.reflect.ClassTag]] is used so that the policy can
   * be registered with an [[EndpointPolicyProvider.EndpointPolicyProvider]].
   *
   * @example
   *   Example usage
   *   {{{
   * package mbauer83.zio_http_authorization
   *
   * import zio.{ZIO, Console, ZIOAppDefault}
   *
   * object PolicyAuthorizationExample extends ZIOAppDefault:
   *   type ResourceType <: Resource[_]
   *   type UserType <: User[_, _]
   *
   *   def getUser: ZIO[Any, Nothing, UserType] = ???
   *   def getResource: ZIO[Any, Nothing, ResourceType] = ???
   *   def getPolicy: ZIO[Any, Nothing, AuthorizationPolicy[UserType, ResourceType]] = ???
   *
   *   override val run = for {
   *     user <- getUser
   *     resource <- getResource
   *     policy <- getPolicy
   *     authorizedResource <- policy.authorized(user)(resource)
   *     _ <-  Console.printLine(authorizedResource)
   *   } yield ()
   *   }}}
   */
  trait AuthorizationPolicy[U <: User[_, _]: ClassTag, R <: Resource[_]: ClassTag]:
    type UserType     = U
    type ResourceType = R

    /**
     * An effect which either fails with a
     * [[UserNotAuthorizedForResourceException]] or succeeds with the resource
     * if the user is authorized to access the resource.
     *
     * If the resource is an [[scala.collection.Iterable]], the effect must fail
     * if and only if the user is not authorized to access any of the resources
     * in the collection. Otherwise, it must succeed with a filtered version of
     * the resource.
     */
    def authorized[Res <: (R | Iterable[R])](user: U)(resource: Res): ZIO[Any, UserNotAuthorizedForResourceException[?], Res]

  /**
   *
   * An [[AuthorizationPolicy]] that always authorizes access to a resource.
   *
   * @example
   *   Create via companion object as in the following example
   *   {{{
   * package mbauer83.zio_http_authorization
   *
   * import zio.{ZIO, Console, ZIOAppDefault}
   *
   * object AllPassAuthExample extends ZIOAppDefault:
   *   type ResourceType <: Resource[_]
   *   type UserType <: User
   *
   *   def getUser: ZIO[Any, Nothing, UserType] = ???
   *   def getResource: ZIO[Any, Nothing, ResourceType] = ???
   *
   *   override val run = for {
   *     user <- getUser
   *     resource <- getResource
   *     policy = AllowAllPolicy[UserType, ResourceType]
   *     authorizedResource <- policy.authorized(user)(resource)
   *     _ <-  Console.printLine(authorizedResource)
   *   } yield ()
   *   }}}
   */
  class AllowAllPolicy[U <: User[_, _]: ClassTag, R <: Resource[_]: ClassTag]
      extends AuthorizationPolicy[U, R]:
    def authorized[Res <: (R | Iterable[R])](user: U)(
          resource: Res,
        ): ZIO[Any, UserNotAuthorizedForResourceException[?], Res] =
          ZIO.succeed(resource)

  /**
   * Create an [[AllowAllPolicy]].
   */
  object AllowAllPolicy:
    def apply[U <: User[_, _], R <: Resource[_]](using
      userTag: ClassTag[U],
      resourceTag: ClassTag[R],
    ): AuthorizationPolicy[U, R] =
      new AllowAllPolicy[U, R]()

  /**
   * An [[AuthorizationPolicy]] that always denies access.
   */
  class DenyAllPolicy[U <: User[_, _]: ClassTag, R <: Resource[_]: ClassTag]
      extends AuthorizationPolicy[U, R]:
    def authorized[Res <: (R | Iterable[R])](user: U)(
          resource: Res,
        ): ZIO[Any, UserNotAuthorizedForResourceException[?], Res] =
          ZIO.fail(UserNotAuthorizedForResourceException(user.id, resource.asInstanceOf[R].descriptor))

  /**
   * Create a [[DenyAllPolicy]]
   */
  object DenyAllPolicy:
    def apply[U <: User[_, _], R <: Resource[_]](using
      userTag: ClassTag[U],
      resourceTag: ClassTag[R],
    ): AuthorizationPolicy[U, R] =
      new DenyAllPolicy[U, R]()

  /** An [[AuthorizationPolicy]] that authorizes access if and only if either of
   *  the following conditions is met:
   *
   *   - The user has the [[Role.SUPER]] role
   *   - The resource's [[Resource.ResourceDescriptor.tenantId]] matches the user's
   *     tenant-id
   *   - The resource's [[Resource.ResourceDescriptor.tenantId]] is empty
   */
  class AuthorizeByTenantIdOrSuperRolePolicy[U <: User[_, _]: ClassTag, R <: Resource[_]: ClassTag]
      extends AuthorizationPolicy[U, R]:
    def authorized[Res <: (R | Iterable[R])](user: U)(
          resource: Res,
        ): ZIO[Any, UserNotAuthorizedForResourceException[?], Res] =
          val isSuper = user.hasRole(Role.SUPER)
          resource match {
            case i: Iterable[_] =>
              val filtered = i
                .filter(
                  r => isSuper || 
                  r.asInstanceOf[R].descriptor.tenantId.equals(user.tenantId) || 
                  r.asInstanceOf[R].descriptor.tenantId.isEmpty
                )
              if filtered.isEmpty then
                ZIO.fail(UserNotAuthorizedForResourceException(user.id, resource.asInstanceOf[R].descriptor))
              else ZIO.succeed(filtered.asInstanceOf[Res])
            case r: R           =>
              if isSuper || r.descriptor.tenantId.equals(user.tenantId) || r.descriptor.tenantId.isEmpty then
                ZIO.succeed(resource)
              else ZIO.fail(UserNotAuthorizedForResourceException(user.id, resource.asInstanceOf[R].descriptor))
          }

  /**
   * Create a [[AuthorizeByTenantIdOrSuperRolePolicy]]
   */
  object AuthorizeByTenantIdOrSuperRolePolicy:
    def apply[U <: User[_, _], R <: Resource[_]](using
      userTag: ClassTag[U],
      resourceTag: ClassTag[R],
    ): AuthorizationPolicy[U, R] =
      new AuthorizeByTenantIdOrSuperRolePolicy[U, R]()

  case class UserNotAuthorizedForResourceException[I <: UserId](
    val userId: I,
    val resourceDescriptor: ResourceDescriptor[_, _],
  ) extends Exception(s"User $userId is not authorized to access resource $resourceDescriptor")

  /** An [[AuthorizationPolicy]] that authorizes access if and only if the user
   *  has all of the required roles and all of the required permissions.
   */
  class GenericAuthorizationPolicy(val requiredRoles: Set[Role.Role], val requiredPermissions: Set[String | Symbol]) extends AuthorizationPolicy[User[?, ?], Resource[?]]:
    def authorized[Res <: (Resource[?] | Iterable[Resource[?]])](user: User[?, ?])(resource: Res): ZIO[Any, UserNotAuthorizedForResourceException[?], Res] =
      // check if user has all required roles and all required permissions matching the resource
      val hasRequiredRoles = requiredRoles.subsetOf(user.roles)
      def hasRequiredPermissions(u: User[?, ?])(r: Resource[?]): Boolean = 
        requiredPermissions.foldLeft[Boolean](true)((acc, description) => acc && u.hasPermissionForResource(description)(r))
      resource match {
        case i: Iterable[_] =>
          val filtered = i
            .filter(r => hasRequiredRoles && hasRequiredPermissions(user)(r.asInstanceOf[Resource[?]]))
          if filtered.isEmpty then
            ZIO.fail(UserNotAuthorizedForResourceException(user.id, i.head.asInstanceOf[Resource[?]].descriptor))
          else ZIO.succeed(filtered.asInstanceOf[Res])
        case r: Resource[?]           =>
          if hasRequiredRoles && hasRequiredPermissions(user)(r) then
            ZIO.succeed(resource)
          else ZIO.fail(UserNotAuthorizedForResourceException(user.id, resource.asInstanceOf[Resource[?]].descriptor))
      }

  /** Secures a parameterized effect with a [[GenericAuthorizationPolicy]].
   *   
   *  Takes a function from [[zio.http.Request Request]], specific type of [[User.User User]], and specific type of  [[Resource.Resource Resource]] to a [[zio.ZIO ZIO effect]]
   *  as well as a set of roles and a set of permissions to produce a new function from a request, the given type of user, and the given type of resource to new ZIO effect
   *  whose failure-type is the union of the original failure-type and [[UserNotAuthorizedForResourceException]].
   * 
   * @example Example usage in a ZIO-http app
   * {{{
   * import zio.http._
   * 
   * class SecuredExampleUsage extends ZIOAppDefault:
   * 
   *   val pathEffect: Request => GenericUser[String, Nothing] => Resource[StringResourceDescriptor] => ZIO[Any, Nothing, String] =
   *     (r: Request) => (u: GenericUser[String, Nothing]) => (res: Resource[StringResourceDescriptor]) => ???
   * 
   *   val app: App[Any] = 
   *     Http.collectZIO[Request] {
   *       case req @ Method.GET -> Root / "test" => {
   *         val securedPathEffectFn = secured(pathEffect)(Set(Role.SUPER), Set("read"))
   *         val user: GenericUser[String, Nothing] = ???
   *         val resource: Resource[StringResourceDescriptor] = ???
   *         val securedEffect = securedPathEffectFn(req)(user)(resource).mapError(_ => Response(Status.Forbidden))
   *         for {
   *           okBodyText <- securedEffect
   *         } yield Response.text(okBodyText)
   *       }
   *     }
   * 
   *   override val run =
   *     Server.serve(app).provide(Server.default)
   * }}}
   */
  def secured[
    BaseResource <: Resource[?], 
    R <: BaseResource |  Iterable[BaseResource], 
    U <: User[?, ?], 
    In, 
    Err, 
    Out, 
    E <: ZIO[In, Err, Out]
  ](
    effect: Request => U => R => E
  )(
    requiredRoles: Set[Role.Role], requiredPermissions: Set[String | Symbol]
  ): Request => U => R => ZIO[In, UserNotAuthorizedForResourceException[?] | Err, Out] = 
    val authPolicy = new GenericAuthorizationPolicy(requiredRoles, requiredPermissions)
    (request: Request) => (user: U) => (resource: R) => {
      authPolicy.authorized(user)(resource).flatMap(authorizedResource => effect(request)(user)(authorizedResource))
    }

