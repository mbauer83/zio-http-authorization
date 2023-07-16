package mbauer83.zio_http_authorization

import User._
import Resource._
import AuthorizationPolicy._
import Role._
import Permission._

import munit.ZSuite
import zio.Clock

class AllowAllPolicySpec extends ZSuite:
  case class MockUser(id: String, tenantId: Option[String], roles: Set[Role], permissions: Set[(Permission, ResourceSelector[_, _])]) extends User[String, String]
  case class MockResource(descriptor: StringResourceDescriptor) extends Resource[StringResourceDescriptor]
  val mockRole01 = Role("mockRole01")

  testZ("AllowAllPolicy must always authorize") {
    val someUser01 = MockUser("someUserId1", Some("someTenantId1"), Set(mockRole01), Set.empty[(Permission, ResourceSelector[_, _])])
    val someUser02 = MockUser("someUserId2", Some("someTenantId2"), Set.empty[Role], Set.empty[(Permission, ResourceSelector[_, _])])
    val someResource = MockResource(StringResourceDescriptor("someResource", "someResourceId", Some("someTenantId1")))
    for {
      authorized01 <- AllowAllPolicy[MockUser, MockResource].authorized(someUser01)(someResource)
      authorized02 <- AllowAllPolicy[MockUser, MockResource].authorized(someUser02)(someResource)
    } yield assert(authorized01.equals(someResource) && authorized02.equals(someResource))
  }