package mbauer83.zio_rbac

import java.util.UUID
import zio.ZIO

/** Resources are the objects to which access is restricted via [[AuthorizationPolicy.AuthorizationPolicy AuthorizationPolicies]].
 *  They are identified by a [[Resource.ResourceDescriptor ResourceDescriptor]] which is generic in its type of 
 *  [[Resource.ResourceId ResourceId]] and [[Resource.TenantId TenantId]].
 */
object Resource:
  /**
   * Resources can be identified by a [[java.util.UUID]], [[scala.Symbol]],
   * `String`, `Number`, or a [[scala.Product]] with a
   * [[scala.Serializable]].
   *
   * @see
   *   [[ResourceDescriptor]]
   */
  type ResourceId = UUID | Symbol | String | Number | Product with Serializable

  /**
   * Tenants can be identified by [[scala.Nothing]], a [[java.util.UUID]],
   * [[scala.Symbol]], `String`, `Number`, or a [[scala.Product]]
   * with a [[scala.Serializable]].
   */
  type TenantId = Nothing | UUID | Symbol | String | Number | Product with Serializable

  /**
   * Describes the type and identity of a resource
   */
  trait ResourceDescriptor[I <: ResourceId, T <: TenantId] extends Product with Serializable:
    type Id = I
    val resourceName: String | Symbol
    val resourceId: I
    val tenantId: Option[T]

  extension (descriptor: ResourceDescriptor[_, _])
    /**
     * Custom equality-checking for
     * [[ResourceDescriptor]]s.
     */
    def equals(other: ResourceDescriptor[_, _]): Boolean =
      descriptor.resourceName.equals(other.resourceName) &&
        descriptor.resourceId.toString().equals(other.resourceId.toString()) &&
        descriptor.tenantId.equals(other.tenantId)

  /**
    * A descriptive representation of a selector on a resource
    */
  trait ResourceSelector[I <: ResourceId, T <: TenantId]:
    val resourceName: String | Symbol
    val resourceId: Option[I]
    val tenantId: Option[T]

  /**
    */
  extension (selector: ResourceSelector[_, _])
    /**
     * `true` if and only if any non-empty field in the selector is matched in the
     * resource.
     */
    def matches(resource: Resource[?]): Boolean =
      selector.resourceName.equals(resource.descriptor.resourceName) &&
        (
          selector.resourceId.isEmpty ||
            selector.resourceId.get.toString().equals(resource.descriptor.resourceId.toString())
        ) &&
        (
          selector.tenantId.isEmpty ||
            selector.tenantId.get.toString().equals(resource.descriptor.tenantId.toString())
        )

  /**
   * A [[ResourceDescriptor]] for a resources and tenants
   * identified by [[java.util.UUID]]s
   */
  case class UUIDResourceDescriptor(
    resourceName: String | Symbol,
    resourceId: UUID,
    tenantId: Option[UUID],
  ) extends ResourceDescriptor[UUID, UUID]

  /**
   * A [[ResourceDescriptor]] for a resources and tenants
   * identified by `String`s
   */
  case class StringResourceDescriptor(
    resourceName: String | Symbol,
    resourceId: String,
    tenantId: Option[String],
  ) extends ResourceDescriptor[String, String]

  /**
   * A [[ResourceDescriptor]] for a resources and tenants
   * identified by `Number`s
   */
  case class NumberResourceDescriptor(
    resourceName: String | Symbol,
    resourceId: Number,
    tenantId: Option[Number],
  ) extends ResourceDescriptor[Number, Number]

  /**
   * A resource in the context of RBAC authorization
   */
  trait Resource[Descriptor <: ResourceDescriptor[_, _]] extends Product with Serializable:
    val descriptor: Descriptor

    /**
     * `true` if and only if this resource has the same identity as the other
     * resource.
     */
    def isSameResourceAs(other: Any): Boolean =
      other match
        case other: Resource[_] => descriptor.equals(other.descriptor)
        case _                  => false
