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

import com.android.tools.idea.editors.gfxtrace.service.atom.AtomList;
import com.android.tools.idea.editors.gfxtrace.service.atom.Range;
import org.jetbrains.annotations.NotNull;

import com.android.tools.rpclib.binary.*;
import com.android.tools.rpclib.schema.*;

import java.io.IOException;

public class Context implements BinaryObject {
  /** ALL is a special context representing all the contexts */
  public static final Context ALL = new Context() {
    @Override
    public boolean contains(long index) { return true; }

    @Override
    public Range[] getRanges(AtomList atoms) {
      return new Range[]{ new Range().setEnd(atoms.getAtoms().length) };
    }
  }.setName("All contexts").setID(ContextID.INVALID);

  public String toString() {
    return myName;
  }

  /** @return true if the atom with the specified index belongs to this context */
  public boolean contains(long index) {
    return Range.contains(myRanges, index);
  }

  public Range[] getRanges(AtomList atoms) {
    return myRanges;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Context context = (Context)o;
    if (myID != null ? !myID.equals(context.myID) : context.myID != null) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return myID != null ? myID.hashCode() : 0;
  }

  //<<<Start:Java.ClassBody:1>>>
  private ContextID myID;
  private String myName;
  private ApiID myApi;
  private Range[] myRanges;

  // Constructs a default-initialized {@link Context}.
  public Context() {}


  public ContextID getID() {
    return myID;
  }

  public Context setID(ContextID v) {
    myID = v;
    return this;
  }

  public String getName() {
    return myName;
  }

  public Context setName(String v) {
    myName = v;
    return this;
  }

  public ApiID getApi() {
    return myApi;
  }

  public Context setApi(ApiID v) {
    myApi = v;
    return this;
  }

  public Range[] getRanges() {
    return myRanges;
  }

  public Context setRanges(Range[] v) {
    myRanges = v;
    return this;
  }

  @Override @NotNull
  public BinaryClass klass() { return Klass.INSTANCE; }


  private static final Entity ENTITY = new Entity("service", "Context", "", "");

  static {
    ENTITY.setFields(new Field[]{
      new Field("ID", new Array("path.ContextID", new Primitive("byte", Method.Uint8), 20)),
      new Field("Name", new Primitive("string", Method.String)),
      new Field("Api", new Array("ApiID", new Primitive("byte", Method.Uint8), 20)),
      new Field("Ranges", new Slice("atom.RangeList", new Struct(Range.Klass.INSTANCE.entity()))),
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
    public BinaryObject create() { return new Context(); }

    @Override
    public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
      Context o = (Context)obj;
      o.myID.write(e);

      e.string(o.myName);
      o.myApi.write(e);

      e.uint32(o.myRanges.length);
      for (int i = 0; i < o.myRanges.length; i++) {
        e.value(o.myRanges[i]);
      }
    }

    @Override
    public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
      Context o = (Context)obj;
      o.myID = new ContextID(d);

      o.myName = d.string();
      o.myApi = new ApiID(d);

      o.myRanges = new Range[d.uint32()];
      for (int i = 0; i <o.myRanges.length; i++) {
        o.myRanges[i] = new Range();
        d.value(o.myRanges[i]);
      }
    }
    //<<<End:Java.KlassBody:2>>>
  }
}
