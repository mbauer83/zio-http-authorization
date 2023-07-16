import mbauer83.zio_http_authorization.User._
import mbauer83.zio_http_authorization.Resource._
import mbauer83.zio_http_authorization.AuthorizationPolicy._
import mbauer83.zio_http_authorization.EndpointPolicyProvider._

import zio.ZIO
import zio.ZIOAppDefault
import zio.http._


class EndpointProviderExample extends ZIOAppDefault:

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
