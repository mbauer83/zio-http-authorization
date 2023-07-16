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

class UserSpec extends ZSuite:
    case class MockUser(id: String, tenantId: Option[String], roles: Set[Role], permissions: Set[(Permission, ResourceSelector[_, _])]) extends User[String, String]
    val readPermission = Permission("READ")
    val writePermission = Permission("WRITE")
    val printPermission = Permission("PRINT")
    val someUser1 = MockUser("someUserId1", Some("someTenantId1"), Set(USER, ADMIN), Set((readPermission, GenericResourceSelector[String, String]("someResource", None, Some("someTenantId1")))))
    val someUser2 = MockUser("someUserId2", Some("someTenantId1"), Set(USER), Set((readPermission, GenericResourceSelector[String, String]("someResource", None, Some("someTenantId1"))), (writePermission, GenericResourceSelector[String, String]("someResource", Some("someResourceId1"), Some("someTenantId1")))))
    case class MockResource(descriptor: StringResourceDescriptor) extends Resource[StringResourceDescriptor]
    val someResource1 = MockResource(StringResourceDescriptor("someResource", "someResourceId", Some("someTenantId1")))
    val someResource2 = MockResource(StringResourceDescriptor("someResource", "someResourceId2", Some("someTenantId1")))


    test("`hasRole` returns true for all and only the contained roles") {
        assert(someUser1.hasRole(USER) && someUser1.hasRole(ADMIN) && !someUser1.hasRole(SUPER))
    }

    test("`hasPermissionForResource` returns true when permission is associated and matches resource") {
        assert(someUser1.hasPermissionForResource("READ")(someResource1))
    }

    test("`hasPermissionForResource` returns false when permission is associated but does not match resource") {
        assert(!someUser1.hasPermissionForResource("PRINT")(someResource2))
    }
