package mbauer83.zio_http_authorization

import User._
import EndpointPolicyProvider._
import Resource._
import AuthorizationPolicy._
import Role._
import Permission._

import munit.ZSuite
import zio._
import zio.http.{Request, Root}
import zio.http.Request
import zio.http.ClientSSLConfig.Default
import zio.http.Method
import zio.http.Body
import zio.http.Headers
import zio.http.URL
import java.net.URI
import zio.http.Path.Segment

class DefaultEndpointPolicyProviderSpec extends ZSuite:
  case class MockResource(descriptor: StringResourceDescriptor) extends Resource[StringResourceDescriptor]
  case class MockUser(id: String, tenantId: Option[String], roles: Set[Role], permissions: Set[(Permission, ResourceSelector[_, _])]) extends User[String, String]

  // A mock AuthorizationPolicy
  class MockDefaultAuthorizationPolicy extends AuthorizationPolicy[User[?, ?], Resource[?]]:
    def authorized[Res <: Resource[?] | Iterable[Resource[?]]](user: User[?, ?])(resource: Res): ZIO[Any, UserNotAuthorizedForResourceException[?], Res] = 
      ZIO.succeed(resource)
  
  // A different mock AuthorizationPolicy
  class MockPathAuthorizationPolicy extends AuthorizationPolicy[MockUser, MockResource]:
    def authorized[Res <: MockResource | Iterable[MockResource]](user: MockUser)(resource: Res): ZIO[Any, UserNotAuthorizedForResourceException[?], Res] = 
      ZIO.succeed(resource)
  
  testZ("DefaultEndpointPolicyProvider must fall back to defaultPolicy when no matching policy is found") {
    val mockUser = MockUser("user1", None, Set.empty, Set.empty)
    val mockResource = MockResource(StringResourceDescriptor("someResouce", "someResourceId01", Some("someTenantId1")))
    val mockDefaultAuthorizationPolicy = new MockDefaultAuthorizationPolicy
    val mockPolicyProvider = new DefaultEndpointPolicyProvider(mockDefaultAuthorizationPolicy, PartialFunction.empty)
    val dummyRequest = Request(Body.empty, Headers.empty, Method.GET, URL.fromURI(new URI("http://localhost:8080/")).get, zio.http.Version.`HTTP/1.1`, None)
    for {
      policy <- mockPolicyProvider.getPolicy[MockUser, MockResource](dummyRequest)
    } yield assert(policy.equals(mockDefaultAuthorizationPolicy))
  }
  
  testZ("DefaultEndpointPolicyProvider must find matching policy") {
    val mockUser = MockUser("user1", None, Set.empty, Set.empty)
    val mockResource = MockResource(StringResourceDescriptor("someResouce", "someResourceId01", Some("someTenantId1")))
    val mockDefaultAuthorizationPolicy = new MockDefaultAuthorizationPolicy
    val mockPathPolicy = new MockPathAuthorizationPolicy
    val mockPolicyProvider = new DefaultEndpointPolicyProvider(mockDefaultAuthorizationPolicy, (req: Request) => req match {
      case r: Request if r.url.toString().contains("/someResource") => mockPathPolicy
    })
    val dummyRequest1 = Request(Body.empty, Headers.empty, Method.GET, URL.fromURI(new URI("http://localhost:8080/")).get, zio.http.Version.`HTTP/1.1`, None)
    val dummyRequest2 = Request(Body.empty, Headers.empty, Method.GET, URL.fromURI(new URI("http://localhost:8080/someResource")).get, zio.http.Version.`HTTP/1.1`, None)
    for {
      policy1 <- mockPolicyProvider.getPolicy[MockUser, MockResource](dummyRequest1)
      policy2 <- mockPolicyProvider.getPolicy[MockUser, MockResource](dummyRequest2)
      _ <- Console.printLine(policy2)
    } yield assert(policy1.equals(mockDefaultAuthorizationPolicy) && policy2.equals(mockPathPolicy))
  }
