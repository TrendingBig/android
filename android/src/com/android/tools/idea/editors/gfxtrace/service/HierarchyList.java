/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * THIS FILE WAS GENERATED BY codergen. EDIT WITH CARE.
 */
package com.android.tools.idea.editors.gfxtrace.service;

import com.google.common.base.Objects;
import org.jetbrains.annotations.NotNull;

import com.android.tools.rpclib.binary.*;
import com.android.tools.rpclib.schema.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public final class HierarchyList implements BinaryObject {
  public Hierarchy get(int index) {
    return myHierarchies[index];
  }

  /** @return the number of hierarchies in this list */
  public int count() {
    return myHierarchies != null ? myHierarchies.length : 0;
  }

  /**
   * @return the hierarchy in this list that matches the context of {@param old},
   *         prioritising any that also matches name. If no similar hierarchies are
   *         found, then the first hierarchy in the list is returned.
   */
  @Nullable
  public Hierarchy findSimilar(@Nullable Hierarchy old) {
    if (count() == 0) {
      return null;
    }
    if (old != null) {
      ContextID ctx = old.getContext();
      String name = old.getName();
      Hierarchy firstMatching = null;
      for (Hierarchy hierarchy : myHierarchies) {
        if (!hierarchy.getContext().equals(ctx)) {
          continue;
        }
        if (hierarchy.getName().equals(name)) {
          return hierarchy; // context and name matches. Winner.
        }
        if (firstMatching == null) {
          firstMatching = hierarchy;
        }
      }
      if (firstMatching != null) {
        return firstMatching;
      }
    }
    return get(0);
  }

  @Nullable
  public Hierarchy firstWithContext(@NotNull ContextID contextID) {
    if (myHierarchies != null) {
      for (Hierarchy hierarchy : myHierarchies) {
        if (Objects.equal(hierarchy.getContext(), contextID)) {
          return hierarchy;
        }
      }
    }
    return null;
  }

  //<<<Start:Java.ClassBody:1>>>
  private Hierarchy[] myHierarchies;

  // Constructs a default-initialized {@link HierarchyList}.
  public HierarchyList() {}


  public Hierarchy[] getHierarchies() {
    return myHierarchies;
  }

  public HierarchyList setHierarchies(Hierarchy[] v) {
    myHierarchies = v;
    return this;
  }

  @Override @NotNull
  public BinaryClass klass() { return Klass.INSTANCE; }


  private static final Entity ENTITY = new Entity("service", "HierarchyList", "", "");

  static {
    ENTITY.setFields(new Field[]{
      new Field("Hierarchies", new Slice("", new Struct(Hierarchy.Klass.INSTANCE.entity()))),
    });
    Namespace.register(Klass.INSTANCE);
  }
  public static void register() {}
  //<<<End:Java.ClassBody:1>>>
  public enum Klass implements BinaryClass {
    //<<<Start:Java.KlassBody:2>>>
    INSTANCE;

    @Override @NotNull
    public Entity entity() { return ENTITY; }

    @Override @NotNull
    public BinaryObject create() { return new HierarchyList(); }

    @Override
    public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
      HierarchyList o = (HierarchyList)obj;
      e.uint32(o.myHierarchies.length);
      for (int i = 0; i < o.myHierarchies.length; i++) {
        e.value(o.myHierarchies[i]);
      }
    }

    @Override
    public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
      HierarchyList o = (HierarchyList)obj;
      o.myHierarchies = new Hierarchy[d.uint32()];
      for (int i = 0; i <o.myHierarchies.length; i++) {
        o.myHierarchies[i] = new Hierarchy();
        d.value(o.myHierarchies[i]);
      }
    }
    //<<<End:Java.KlassBody:2>>>
  }
}
