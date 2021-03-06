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
package com.android.tools.idea.editors.gfxtrace.service.stringtable;

import org.jetbrains.annotations.NotNull;

import com.android.tools.rpclib.binary.*;
import com.android.tools.rpclib.schema.*;

import java.io.IOException;

public final class Parameter extends Node implements BinaryObject {
  @Override
  public String getString(java.util.Map<String, BinaryObject> arguments) {
    Object argument = arguments.get(myKey);
    if (argument == null) {
      return "<" + myKey + ">";
    }
    if (myFormatter != null) {
      return myFormatter.getString(argument);
    }
    return argument.toString();
  }

  //<<<Start:Java.ClassBody:1>>>
  private Formatter myFormatter;
  private String myKey;

  // Constructs a default-initialized {@link Parameter}.
  public Parameter() {}


  public Formatter getFormatter() {
    return myFormatter;
  }

  public Parameter setFormatter(Formatter v) {
    myFormatter = v;
    return this;
  }

  public String getKey() {
    return myKey;
  }

  public Parameter setKey(String v) {
    myKey = v;
    return this;
  }

  @Override @NotNull
  public BinaryClass klass() { return Klass.INSTANCE; }


  private static final Entity ENTITY = new Entity("stringtable", "Parameter", "", "");

  static {
    ENTITY.setFields(new Field[]{
      new Field("Formatter", new Interface("Formatter")),
      new Field("Key", new Primitive("string", Method.String)),
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
    public BinaryObject create() { return new Parameter(); }

    @Override
    public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
      Parameter o = (Parameter)obj;
      e.object(o.myFormatter.unwrap());
      e.string(o.myKey);
    }

    @Override
    public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
      Parameter o = (Parameter)obj;
      o.myFormatter = Formatter.wrap(d.object());
      o.myKey = d.string();
    }
    //<<<End:Java.KlassBody:2>>>
  }
}
