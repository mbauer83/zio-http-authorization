package mbauer83.zio_http_authorization

import User._
import Resource._
import AuthorizationPolicy._
import Role._
import Permission._

import munit.ZSuite
import zio.Clock

class AuthorizeByTenantIdOrSuperRolePolicySpec extends ZSuite:
  case class MockUser(id: String, tenantId: Option[String], roles: Set[Role], permissions: Set[(Permission, ResourceSelector[_, _])]) extends User[String, String]
  case class MockResource(descriptor: StringResourceDescriptor) extends Resource[StringResourceDescriptor]
  val someResource = MockResource(StringResourceDescriptor("someResource", "someResourceId", Some("someTenantId1")))

  testZ("AuthorizeByTenantIdOrSuperRolePolicy must authorize user with any role and matching tenant-id.") {
    val mockRole01 = Role("mockRole01")
    val someUser01 = MockUser("someUserId1", Some("someTenantId1"), Set(mockRole01), Set.empty[(Permission, ResourceSelector[_, _])])
    for {
      authorizedResource <- AuthorizeByTenantIdOrSuperRolePolicy[MockUser, MockResource].authorized(someUser01)(someResource)
    } yield assert(authorizedResource.equals(someResource))
  }

  testZ("AuthorizeByTenantIdOrSuperRolePolicy must authorize user with any tenant-id and SUPER role.") {
    val someUser02 = MockUser("someUserId2", Some("someTenantId2"), Set(SUPER), Set.empty[(Permission, ResourceSelector[_, _])])
    for {
      authorizedResource <- AuthorizeByTenantIdOrSuperRolePolicy[MockUser, MockResource].authorized(someUser02)(someResource)
    } yield assert(authorizedResource.equals(someResource))
  }

  testZ("AuthorizeByTenantIdOrSuperRolePolicy must not authorize non-SUPER user without matching tenant-id.") {
  val someUser03 = MockUser("someUserId2", Some("someTenantId2"), Set.empty[Role], Set.empty[(Permission, ResourceSelector[_, _])])
  for {
        exception <- AuthorizeByTenantIdOrSuperRolePolicy[MockUser, MockResource].authorized(someUser03)(someResource).flip
    } yield assert(exception.isInstanceOf[UserNotAuthorizedForResourceException[_]])
  }
