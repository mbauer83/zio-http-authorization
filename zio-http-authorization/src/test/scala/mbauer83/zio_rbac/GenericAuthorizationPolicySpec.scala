package mbauer83.zio_http_authorization

import User._
import Resource._
import AuthorizationPolicy._
import Role._
import Permission._

import munit.ZSuite
import zio.Clock
import zio.http.Request
import zio.http.Method
import zio.http.Body
import zio.http.Path.Segment.Root
import zio.http.URL
import zio.ZIO

class GenericAuthorizationPolicySpec extends ZSuite:
  case class MockUser(id: String, tenantId: Option[String], roles: Set[Role], permissions: Set[(Permission, ResourceSelector[_, _])]) extends User[String, String]
  case class MockResource(descriptor: StringResourceDescriptor) extends Resource[StringResourceDescriptor]
  val someResource = MockResource(StringResourceDescriptor("someResource", "someResourceId", Some("someTenantId1")))
  val readPermission = Permission("READ")
  val writePermission = Permission("WRITE")
  val user1 = MockUser(
    "someUser01", 
    Some("someTenantId1"), 
    Set(ADMIN), 
    Set(
        (readPermission, GenericResourceSelector[String, String]("someResource", None, Some("someTenantId1"))),
        (writePermission, GenericResourceSelector[String, String]("someResource", None, Some("someTenantId1")))
    )
  )
  val user2 = MockUser(
    "someUser02", 
    Some("someTenantId1"), 
    Set(USER), 
    Set(
        (readPermission, GenericResourceSelector[String, String]("someResource", Some("someResourceId2"), Some("someTenantId1")))
    )
  )

  val effectFn = (req: Request) => (u: MockUser) => (res: MockResource) => ZIO.succeed("OK")


  testZ("GenericAuthorizationPolicy tests for all and only the specified roles") {
    val policy = GenericAuthorizationPolicy(Set(ADMIN), Set.empty[String | Symbol])
    for {
      resource  <- policy.authorized(user1)(someResource)
      failure   <- policy.authorized(user2)(someResource).flip
    } yield assert(resource.isInstanceOf[MockResource] && failure.isInstanceOf[UserNotAuthorizedForResourceException[_]])
  }

  testZ("GenericAuthorizationPolicy tests for all and only the specified permissions") {
    val policy = GenericAuthorizationPolicy(Set.empty[Role], Set("WRITE"))
    for {
      resource  <- policy.authorized(user1)(someResource)
      failure   <- policy.authorized(user2)(someResource).flip
    } yield assert(resource.isInstanceOf[MockResource] && failure.isInstanceOf[UserNotAuthorizedForResourceException[_]])
  }

