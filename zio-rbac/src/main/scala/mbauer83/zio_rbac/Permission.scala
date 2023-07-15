package mbauer83.zio_rbac

import Resource._

/** Permissions define specific (kinds of) actions that may be performed on a [[Resource.Resource Resource]]
 *  by a [[User.User User]] to which the permission is assigned.
 * 
 *  Permissions contain a name and a [[Permission.PermissionResourceDescriptor PermissionResourceDescriptor]] to
 *  enable matching against [[Resource.Resource Resources]].
 */
object Permission:
  /**
   * Describes the type and optionally the identity of a [[Resource.Resource Resource]] for
   * matching in a [[Permission]].
   */
  case class PermissionResourceDescriptor[R <: ResourceId, T <: TenantId](
    resourceName: String | Symbol,
    resourceId: Option[R],
    tenantId: Option[T],
  ):
    /**
     * Checks whether a given [[Resource.Resource Resource]] matches this descriptor.
     */
    def matchesResource(resource: Resource[?]): Boolean =
      resource.descriptor.resourceName.equals(resourceName) &&
        (
          resourceId.isEmpty ||
            resourceId.get.toString().equals(resource.descriptor.resourceId.toString())
        ) &&
        (
          tenantId.isEmpty ||
            tenantId.get.toString().equals(resource.descriptor.tenantId.toString())
        )

  /**
   * A specific permission held by a [[User.User User]].
   */
  case class Permission(name: String | Symbol, resourceDescriptor: PermissionResourceDescriptor[_, _]):
    /**
     * Cheks whether a given [[Resource.Resource Resource]] matches this permission.
     */
    def matchesResource(resource: Resource[?]): Boolean =
      resourceDescriptor.matchesResource(resource)
