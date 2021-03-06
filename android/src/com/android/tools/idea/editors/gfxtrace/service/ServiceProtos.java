// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: gapid/service/service.proto

package com.android.tools.idea.editors.gfxtrace.service;

public final class ServiceProtos {
  private ServiceProtos() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  /**
   * Protobuf enum {@code service.WireframeMode}
   *
   * <pre>
   * WireframeMode is an enumerator of wireframe modes that can be used by
   * RenderSettings.
   * </pre>
   */
  public enum WireframeMode
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>None = 0;</code>
     *
     * <pre>
     * None indicates that nothing should be drawn in wireframe.
     * </pre>
     */
    None(0, 0),
    /**
     * <code>Overlay = 1;</code>
     *
     * <pre>
     * Overlay indicates that the single draw call should be overlayed
     * with the wireframe of the mesh.
     * </pre>
     */
    Overlay(1, 1),
    /**
     * <code>All = 2;</code>
     *
     * <pre>
     * All indicates that all draw calls should be displayed in wireframe.
     * </pre>
     */
    All(2, 2),
    UNRECOGNIZED(-1, -1),
    ;

    /**
     * <code>None = 0;</code>
     *
     * <pre>
     * None indicates that nothing should be drawn in wireframe.
     * </pre>
     */
    public static final int None_VALUE = 0;
    /**
     * <code>Overlay = 1;</code>
     *
     * <pre>
     * Overlay indicates that the single draw call should be overlayed
     * with the wireframe of the mesh.
     * </pre>
     */
    public static final int Overlay_VALUE = 1;
    /**
     * <code>All = 2;</code>
     *
     * <pre>
     * All indicates that all draw calls should be displayed in wireframe.
     * </pre>
     */
    public static final int All_VALUE = 2;


    public final int getNumber() {
      if (index == -1) {
        throw new java.lang.IllegalArgumentException(
            "Can't get the number of an unknown enum value.");
      }
      return value;
    }

    public static WireframeMode valueOf(int value) {
      switch (value) {
        case 0: return None;
        case 1: return Overlay;
        case 2: return All;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<WireframeMode>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static final com.google.protobuf.Internal.EnumLiteMap<
        WireframeMode> internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<WireframeMode>() {
            public WireframeMode findValueByNumber(int number) {
              return WireframeMode.valueOf(number);
            }
          };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      return getDescriptor().getValues().get(index);
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return com.android.tools.idea.editors.gfxtrace.service.ServiceProtos.getDescriptor().getEnumTypes().get(0);
    }

    private static final WireframeMode[] VALUES = values();

    public static WireframeMode valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      if (desc.getIndex() == -1) {
        return UNRECOGNIZED;
      }
      return VALUES[desc.getIndex()];
    }

    private final int index;
    private final int value;

    private WireframeMode(int index, int value) {
      this.index = index;
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:service.WireframeMode)
  }


  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\033gapid/service/service.proto\022\007service*/" +
      "\n\rWireframeMode\022\010\n\004None\020\000\022\013\n\007Overlay\020\001\022\007" +
      "\n\003All\020\002B@\n/com.android.tools.idea.editor" +
      "s.gfxtrace.serviceB\rServiceProtosb\006proto" +
      "3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
  }

  // @@protoc_insertion_point(outer_class_scope)
}
