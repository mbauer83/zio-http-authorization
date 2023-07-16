# Zio-http-authorization

Zio-http-authorization is a library for basic effectful role-based access-control in ZIO-http applications.

## Basic concepts

### Role
A role is a effectively a tag that is associated with [Users](#user) in a many-to-many fashion and potentially required by many [AuthorizationPolicies](#authorizationpolicy).

This library defines the following default roles:

  - `SUPER` - intended to be assigned to users with system-wide, completely unrestricted access.
  - `ADMIN` - intended for users with administrative permissions, e.g. to manage other users.
  - `USER`  - intended for standard users.

### Permission
A permission is again a form of tag, but this time representing a (kind of) action a user may be permitted to perform on a [Resource](#resource). It is also associated with [Users](#user) in a many-to-many fashion, but this time tupled with a descriptive [ResourceSelector](#resourceselector).
Thus, an [AuthorizationPolicy](#authorizationpolicy) may require a user to hold a certain permission for the specific resource in order to be authorized.

### Resource
A resource is a type of object that can be protected by [AuthorizationPolicies](#authorizationpolicy). It is usually a class that represents an aggregate-root in a domain model. In the bounded context of authorization, this library defines a resource via a trait that extends `Product with Serializable` and is generic in the type of its descriptor. The desriptor-type is constrained to be an instance of a [ResourceDescriptor](#resourcedescriptor).

### ResourceDescriptor
A resource-descriptor is used to access metadata about a resource at runtime. It is defined as a trait which extends `Product with Serializable`, is generic in its type of user-id and tenant-id. It defines the following type-members:

  - `IdType` - the type of the resource's id
  - `TenantIdType` - the type of the resource's tenant-id

Additionaly, it defines the following values
  - `resourceName` of type `String | Symbol`
  - `resourceId` of the generic id-type
  - `tenantId` an `Option` of the generic tenant-id type. This value is an `Option` to support single-tenant applications and resources which do not belong to a specific tenant.

### ResourceSelector
A resource-selector is simiar to a [ResourceDescriptor](#resourcedescriptor), except that the `resourceId` is also an `Option`. If any option is `None`, the selector will match resources with any value for these fields. Otherwise, the selector will only match resources with the same value for these fields.

### User
A user is the subject of authorization and the holder of [Roles](#role) and [Permissions](#permission), which are associated to the user together with a [ResourceSelector](#resourceselector). A user is identified by a unique id of a generic type constrained to the the union `UUID | Symbol | String | Number | Product with Serializable` and by an optional tenant-id of a generic type constrained to the same union. The tenant-id is optional to support single-tenant applications and users (e.g. SUPER-users) which do not belong to a specific tenant.

### AuthorizationPolicy
An authorization-policy encapsulates a specific authorization-logic for a specific type of resource and specific type of user. It is generic in its type of [User](#user) and [Resource](#resource) and defines a single `authorize`-method which takes a resource of the generic type or an `Iterable` thereof, also takes a user of the generic type and returns a `ZIO`-effect which may either fail with a `UserNotAuthorizedForResourceException` or succeed with the (filtered) resource.

This library provides a `GenericAuthorizationPolicy` which takes a set of required roles and a set of descriptions of required permissions, and provides an authorization-effect which succeeds if the user holds all required roles and all required permissions for the resource and fails otherwise.

Furthermore, the `AuthorizationPolicy` namespaces provides a `secured` function. This is a higher-order-function which takes a `ZIO`-effect parameterized by a `zio.http.Request`, a specific type of [User](#user) and a specific type of [Resource](#resource), as well as a set of [Roles](#role) and a set of [Permissions](#permission) to produce a new parameterized effect which will first authorize the user for the resource and then execute the original effect if authorization succeeds, and otherwise fail with a `UserNotAuthorizedForResourceException`.
Thus, it modifies the effect's failure-type to be a union of its original failure-type and `UserNotAuthorizedForResourceException`.

### EndpointPolicyProvider
An EndpointPolicyProvider is used to procure an [AuthorizationPolicy](#authorizationpolicy) given a `zio.http.Request`. It holds a default `AuthorizationPolicy` which is applicable to all types of users and resources and is used when no other matching policy can be found. It also permits registration of additional policies as partial functions from `zio.http.Request` to `AuthorizationPolicy` which are applied in the order of their registration / definition. The first matching policy (or the default) is used to authorize the request.

## Usage
[AuthorizationPolicies](#authorizationpolicy) can either be procured from within an endpoint via an [EndpointPolicyProvider](#endpointpolicyprovider) or via `secured` function. The latter is useful when you wish to keep the definition of security-requirements for an enpoint within the endpoint's definition. The former is useful when a way of registering policies as a separate aspect is required.

The following exemplifies usage of the `secured` function:

```scala
import mbauer83.zio_http_authorization.Role
import mbauer83.zio_http_authorization.User._
import mbauer83.zio_http_authorization.Resource._
import mbauer83.zio_http_authorization.AuthorizationPolicy._

import zio.http._
import zio.ZIO
import zio.ZIOAppDefault


class SecuredExampleUsage extends ZIOAppDefault:

  val pathEffect: Request => GenericUser[String, Nothing] => Resource[StringResourceDescriptor] => ZIO[Any, Nothing, String] =
    (r: Request) => (u: GenericUser[String, Nothing]) => (res: Resource[StringResourceDescriptor]) => ???

  val app: App[Any] = 
    Http.collectZIO[Request] {
      case req @ Method.GET -> Root / "test" => {
        val securedPathEffectFn = secured(pathEffect)(Set(Role.SUPER), Set("read"))
        val user: GenericUser[String, Nothing] = ???
        val resource: Resource[StringResourceDescriptor] = ???
        val securedEffect = securedPathEffectFn(req)(user)(resource)

        (for {
          okBodyText <- securedEffect
        } yield Response.text(okBodyText)) orElse ZIO.succeed(Response(Status.Forbidden))
      }
    }

  override val run =
    Server.serve(app).provide(Server.default)
```

This is an example for usage of an `EndpointPolicyProvider`:

```scala
package mbauer83.zio_http_authorization

import mbauer83.zio_http_authorization.User._
import mbauer83.zio_http_authorization.Resource._
import mbauer83.zio_http_authorization.AuthorizationPolicy._
import mbauer83.zio_http_authorization.EndpointPolicyProvider._

import zio.ZIO
import zio.ZIOAppDefault
import zio.http._


class ProviderExampleUsage extends ZIOAppDefault:

  val examplePolicy = GenericAuthorizationPolicy(Set(Role.USER), Set("read"))

  val policyProvider = new DefaultEndpointPolicyProvider(AllowAllPolicy[User[?, ?], Resource[?]], (req: Request) => req match {
      case r: Request if r.url.toString().contains("/someResource") => examplePolicy.asInstanceOf[AuthorizationPolicy[User[?, ?], Resource[?]]]
    })

  val effectFn: (Request, GenericUser[String, Nothing], Resource[StringResourceDescriptor]) => ZIO[Any, Nothing, String] =
    (r: Request, u: GenericUser[String, Nothing], res: Resource[StringResourceDescriptor]) => ???

  val app: App[Any] = 
    Http.collectZIO[Request] {
      case req @ Method.GET -> Root / "someResource" => {
        val user: GenericUser[String, Nothing] = ???
        val resource: Resource[StringResourceDescriptor] = ???

        (for {
          authPolicy         <- policyProvider.getPolicy[GenericUser[?, ?], Resource[?]](req)  
          authorizedResource <- authPolicy.authorized(user)(resource)
          okBodyText         <- effectFn(req, user, authorizedResource)
        } yield Response.text(okBodyText)) orElse ZIO.succeed(Response(Status.Forbidden))
      }
    }

  override val run =
    Server.serve(app).provide(Server.default)
```