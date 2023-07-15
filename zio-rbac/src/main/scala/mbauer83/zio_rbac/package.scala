package mbauer83

/**
 * A simple Role Based Access Control (RBAC) implementation for use with
 * ZIO-http. Provides the means to declare and manage [[Resource.Resource Resources]],
 * [[Permission.Permission Permissions]], [[Role.Role Roles]], [[User.User Users]] and 
 * [[AuthorizationPolicy.AuthorizationPolicy AuthorizationPolicies]].
 *
 * These policies can then be registered with an [[EndpointPolicyProvider.EndpointPolicyProvider EndpointPolicyProvider]] as
 * partial functions from [[zio.http.Request Request]] to [[AuthorizationPolicy.AuthorizationPolicy AuthorizationPolicy]].
 *
 * The [[EndpointPolicyProvider.EndpointPolicyProvider EndpointPolicyProvider]] (e.g. [[EndpointPolicyProvider.DefaultEndpointPolicyProvider DefaultEndpointPolicyProvider]]) can then be used to get a matching
 * policy for a given [[zio.http.Request Request]].
 */
package object zio_rbac
