package mbauer83.zio_http_authorization

import User._
import Resource._
import AuthorizationPolicy._
import Role._
import Permission._

import munit.ZSuite
import zio.http.Request
import zio.http.Method
import zio.http.Body
import zio.http.URL
import zio.ZIO

class SecuredSpec extends ZSuite:
  case class MockUser(id: String, tenantId: Option[String], roles: Set[Role], permissions: Set[(Permission, ResourceSelector[_, _])]) extends User[String, String]
  case class MockResource(descriptor: StringResourceDescriptor) extends Resource[StringResourceDescriptor]
  val someResource1 = MockResource(StringResourceDescriptor("someResource", "someResourceId", Some("someTenantId1")))
  val someResource2 = MockResource(StringResourceDescriptor("someResource", "someResourceId2", Some("someTenantId1")))
  val someResourceSeq = Seq(someResource1, someResource2)
  val readPermission = Permission("READ")
  val writePermission = Permission("WRITE")
  val user1 = MockUser(
    "someUser01", 
    Some("someTenantId1"), 
    Set(USER, ADMIN), 
    Set(
        (readPermission, GenericResourceSelector[String, String]("someResource", None, Some("someTenantId1"))),
        (writePermission, GenericResourceSelector[String, String]("someResource", Some("someResourceId"), Some("someTenantId1")))
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

  val effectFn = (req: Request) => (u: MockUser) => (res: MockResource | Iterable[MockResource]) => ZIO.succeed(res)


  testZ("`secured` tests for all and only the specified roles") {
    val securedEffect = secured(effectFn)(Set(ADMIN), Set.empty[String | Symbol])
    for {
      result  <- securedEffect(Request.default(Method.GET, URL.empty, Body.empty))(user1)(someResource1)
      failure <- securedEffect(Request.default(Method.GET, URL.empty, Body.empty))(user2)(someResource1).flip
    } yield assert(result.equals(someResource1) && failure.isInstanceOf[UserNotAuthorizedForResourceException[_]])
  }

  testZ("`secured` tests for all and only the specified permissions") {
    val securedEffect = secured(effectFn)(Set.empty[Role], Set("WRITE"))
    for {
      result  <- securedEffect(Request.default(Method.GET, URL.empty, Body.empty))(user1)(someResource1)
      failure <- securedEffect(Request.default(Method.GET, URL.empty, Body.empty))(user2)(someResource1).flip
    } yield assert(result.equals(someResource1) && failure.isInstanceOf[UserNotAuthorizedForResourceException[_]])
  }

  testZ("`secured` filters Iterables of resources") {
    val securedEffect = secured(effectFn)(Set(USER), Set("WRITE"))
    for {
      result  <- securedEffect(Request.default(Method.GET, URL.empty, Body.empty))(user1)(someResourceSeq)
    } yield assertEquals(result, Seq(someResource1))
  }

  testZ("`secured` on Iterables of resources fails when filtering to empty Iterable") {
    val securedEffect = secured(effectFn)(Set(USER), Set("WRITE"))
    for {
      failure <- securedEffect(Request.default(Method.GET, URL.empty, Body.empty))(user2)(someResourceSeq).flip
    } yield assert(failure.isInstanceOf[UserNotAuthorizedForResourceException[_]])
  }

