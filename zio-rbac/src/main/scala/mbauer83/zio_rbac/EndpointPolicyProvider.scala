package mbauer83.zio_rbac

import Resource._
import AuthorizationPolicy._
import User._


import zio.http.URL
import zio.http.Headers
import zio.ZIO
import zio.http.Method
import zio.http._
import scala.reflect.TypeTest
import scala.reflect.ClassTag
import scala.compiletime.summonFrom
import zio.http.Path
import zio.http.Path.Segment
import zio.http.Request
import java.net.URI

/**
  * An EndpointPolicyProvider is used to retrieve a matching * [[AuthorizationPolicy.AuthorizationPolicy AuthorizationPolicy]]
  * for a given [[zio.http.Request Request]] from a set of policies, each registered with a partial function 
  * from [[zio.http.Request Request]] to the policy.
  */
object EndpointPolicyProvider:
  /**
   * Permits effectful retrieval of a matching [[AuthorizationPolicy.AuthorizationPolicy AuthorizationPolicy]] given a [[zio.http.Request Request]]
   * as well as effectful registration of [[AuthorizationPolicy.AuthorizationPolicy AuthorizationPolicies]] using a partial function from
   * [[zio.http.Request Request]] to a policy.
   * 
   * Both the retrieval and registration methods are polymorphic in the types of the [[User.User User]] and [[Resource.Resource Resource]]
   * to which the policy applies.
   * 
   * A default policy is also required. This is used as a fallback when no matching policy is found.
   * 
   * @note To fail safe when no matching policy is found, it is strongly recommended to set the provided 
   * [[AuthorizationPolicy.DenyAllPolicy DenyAllPolicy]] as the default policy.
   */
  trait EndpointPolicyProvider:
    val defaultPolicy: AuthorizationPolicy[?, ?]

    /**
     * An effect which always succeds with an appropriate [[AuthorizationPolicy.AuthorizationPolicy AuthorizationPolicy]] given a
     * [[zio.http.Request]], with fallback to the [[defaultPolicy]] if no matching policy is found.
     */
    def getPolicy[U <: User[_, _]: ClassTag, R <: Resource[_]: ClassTag](
      req: Request,
    ): ZIO[Any, Nothing, AuthorizationPolicy[U, R]]

    /**
     * Registers an [[AuthorizationPolicy.AuthorizationPolicy AuthorizationPolicy]] with a partial function.
     */
    def registerPolicy[U <: User[_, _]: ClassTag, R <: Resource[_]: ClassTag](
      matcher: PartialFunction[Request, AuthorizationPolicy[U, R]],
    ): ZIO[Any, Nothing, EndpointPolicyProvider]

  /**
   * A default implementation of an [[EndpointPolicyProvider]].
   */
  case class DefaultEndpointPolicyProvider(
    defaultPolicy: AuthorizationPolicy[User[?, ?], Resource[?]],
    registeredPolicies: PartialFunction[Request, AuthorizationPolicy[_, _]],
  ) extends EndpointPolicyProvider:
    inline def getPolicy[U <: User[_, _]: ClassTag, R <: Resource[_]: ClassTag](
      req: Request,
    ): ZIO[Any, Nothing, AuthorizationPolicy[U, R]] =
      val policy           = registeredPolicies lift req
      val defaultPolicyZIO = ZIO.succeed(defaultPolicy.asInstanceOf[AuthorizationPolicy[U, R]])
      policy match
        case Some(policy) =>
          // if TypeTests can be summoned for user and resource, return the policy
          summonFrom {
            case test: TypeTest[policy.UserType, U] =>
              summonFrom {
                case test: TypeTest[policy.ResourceType, R] =>
                  ZIO.succeed(policy.asInstanceOf[AuthorizationPolicy[U, R]])
                case _                                      =>
                  defaultPolicyZIO
              }
            case _                                  =>
              defaultPolicyZIO
          }
        case None         =>
          defaultPolicyZIO

    def registerPolicy[U <: User[_, _]: ClassTag, R <: Resource[_]: ClassTag](
      matcher: PartialFunction[Request, AuthorizationPolicy[U, R]]
    ): ZIO[Any, Nothing, EndpointPolicyProvider] =
      ZIO.succeed(DefaultEndpointPolicyProvider(defaultPolicy, registeredPolicies orElse matcher))