import mbauer83.zio_http_authorization.Role
import mbauer83.zio_http_authorization.User._
import mbauer83.zio_http_authorization.Resource._
import mbauer83.zio_http_authorization.AuthorizationPolicy._

import zio.http._
import zio.ZIO
import zio.ZIOAppDefault


class SecuredExample extends ZIOAppDefault:

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