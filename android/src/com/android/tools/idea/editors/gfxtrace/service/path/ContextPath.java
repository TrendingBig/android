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
package com.android.tools.idea.editors.gfxtrace.service.path;

import com.android.tools.idea.editors.gfxtrace.service.ContextID;
import org.jetbrains.annotations.NotNull;

import com.android.tools.rpclib.binary.*;
import com.android.tools.rpclib.schema.*;

import java.io.IOException;

public final class ContextPath  extends Path {
  @Override
  public String getSegmentString() {
    return "<" + myID.toString() + ">";
  }

  @Override
  public void appendSegmentToPath(StringBuilder builder) {
    builder.append(getSegmentString());
  }

  @Override
  public Path getParent() {
    return myContexts;
  }

  //<<<Start:Java.ClassBody:1>>>
  private ContextsPath myContexts;
  private ContextID myID;

  // Constructs a default-initialized {@link ContextPath}.
  public ContextPath() {}


  public ContextsPath getContexts() {
    return myContexts;
  }

  public ContextPath setContexts(ContextsPath v) {
    myContexts = v;
    return this;
  }

  public ContextID getID() {
    return myID;
  }

  public ContextPath setID(ContextID v) {
    myID = v;
    return this;
  }

  @Override @NotNull
  public BinaryClass klass() { return Klass.INSTANCE; }


  private static final Entity ENTITY = new Entity("path", "Context", "", "");

  static {
    ENTITY.setFields(new Field[]{
      new Field("Contexts", new Pointer(new Struct(ContextsPath.Klass.INSTANCE.entity()))),
      new Field("ID", new Array("ContextID", new Primitive("byte", Method.Uint8), 20)),
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
    public BinaryObject create() { return new ContextPath(); }

    @Override
    public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
      ContextPath o = (ContextPath)obj;
      e.object(o.myContexts);
      o.myID.write(e);

    }

    @Override
    public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
      ContextPath o = (ContextPath)obj;
      o.myContexts = (ContextsPath)d.object();
      o.myID = new ContextID(d);

    }
    //<<<End:Java.KlassBody:2>>>
  }
}
