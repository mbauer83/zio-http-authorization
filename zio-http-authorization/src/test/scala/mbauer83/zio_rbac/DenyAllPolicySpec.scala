package mbauer83.zio_http_authorization

import User._
import Resource._
import AuthorizationPolicy._
import Role._
import Permission._

import munit.ZSuite
import zio.Clock

class DenyAllPolicySpec extends ZSuite:
  case class MockUser(id: String, tenantId: Option[String], roles: Set[Role], permissions: Set[(Permission, ResourceSelector[_, _])]) extends User[String, String]
  case class MockResource(descriptor: StringResourceDescriptor) extends Resource[StringResourceDescriptor]
  val mockRole01 = Role("mockRole01")

  testZ("DenyAllPolicy must always deny") {
    val someUser01 = MockUser("someUserId1", Some("someTenantId1"), Set(mockRole01), Set.empty[(Permission, ResourceSelector[_, _])])
    val someUser02 = MockUser("someUserId2", Some("someTenantId2"), Set.empty[Role], Set.empty[(Permission, ResourceSelector[_, _])])
    val someResource = MockResource(StringResourceDescriptor("someResource", "someResourceId", Some("someTenantId1")))
    for {
      exception1 <- DenyAllPolicy[MockUser, MockResource].authorized(someUser01)(someResource).flip
      exception2 <- DenyAllPolicy[MockUser, MockResource].authorized(someUser02)(someResource).flip
    } yield assert(exception1.isInstanceOf[UserNotAuthorizedForResourceException[_]] && exception2.isInstanceOf[UserNotAuthorizedForResourceException[_]])
  }