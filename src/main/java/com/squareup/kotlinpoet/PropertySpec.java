/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import kotlin.reflect.KClass;

import static com.squareup.kotlinpoet.Util.checkArgument;
import static com.squareup.kotlinpoet.Util.checkNotNull;
import static com.squareup.kotlinpoet.Util.checkState;

/** A generated property declaration. */
public final class PropertySpec {
  public final TypeName type;
  public final String name;
  public final CodeBlock kdoc;
  public final List<AnnotationSpec> annotations;
  public final Set<Modifier> modifiers;
  public final CodeBlock initializer;

  private PropertySpec(Builder builder) {
    this.type = checkNotNull(builder.type, "type == null");
    this.name = checkNotNull(builder.name, "name == null");
    this.kdoc = builder.kdoc.build();
    this.annotations = Util.immutableList(builder.annotations);
    this.modifiers = Util.immutableSet(builder.modifiers);
    this.initializer = (builder.initializer == null)
        ? CodeBlock.builder().build()
        : builder.initializer;
  }

  public boolean hasModifier(Modifier modifier) {
    return modifiers.contains(modifier);
  }

  void emit(CodeWriter codeWriter, Set<Modifier> implicitModifiers) throws IOException {
    codeWriter.emitKdoc(kdoc);
    codeWriter.emitAnnotations(annotations, false);
    codeWriter.emitModifiers(modifiers, implicitModifiers);
    codeWriter.emit("%L: %T", name, type);
    if (!initializer.isEmpty()) {
      codeWriter.emit(" = ");
      codeWriter.emit(initializer);
    }
    codeWriter.emit(";\n");
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  @Override public int hashCode() {
    return toString().hashCode();
  }

  @Override public String toString() {
    StringWriter out = new StringWriter();
    try {
      CodeWriter codeWriter = new CodeWriter(out);
      emit(codeWriter, Collections.<Modifier>emptySet());
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  public static Builder builder(TypeName type, String name, Modifier... modifiers) {
    checkNotNull(type, "type == null");
    checkArgument(SourceVersion.isName(name), "not a valid name: %s", name);
    return new Builder(type, name)
        .addModifiers(modifiers);
  }

  public static Builder builder(Type type, String name, Modifier... modifiers) {
    return builder(TypeName.get(type), name, modifiers);
  }

  public static Builder builder(KClass<?> type, String name, Modifier... modifiers) {
    return builder(TypeName.get(type), name, modifiers);
  }

  public Builder toBuilder() {
    Builder builder = new Builder(type, name);
    builder.kdoc.add(kdoc);
    builder.annotations.addAll(annotations);
    builder.modifiers.addAll(modifiers);
    builder.initializer = initializer.isEmpty() ? null : initializer;
    return builder;
  }

  public static final class Builder {
    private final TypeName type;
    private final String name;

    private final CodeBlock.Builder kdoc = CodeBlock.builder();
    private final List<AnnotationSpec> annotations = new ArrayList<>();
    private final List<Modifier> modifiers = new ArrayList<>();
    private CodeBlock initializer = null;

    private Builder(TypeName type, String name) {
      this.type = type;
      this.name = name;
    }

    public Builder addKdoc(String format, Object... args) {
      kdoc.add(format, args);
      return this;
    }

    public Builder addKdoc(CodeBlock block) {
      kdoc.add(block);
      return this;
    }

    public Builder addAnnotations(Iterable<AnnotationSpec> annotationSpecs) {
      checkArgument(annotationSpecs != null, "annotationSpecs == null");
      for (AnnotationSpec annotationSpec : annotationSpecs) {
        this.annotations.add(annotationSpec);
      }
      return this;
    }

    public Builder addAnnotation(AnnotationSpec annotationSpec) {
      this.annotations.add(annotationSpec);
      return this;
    }

    public Builder addAnnotation(ClassName annotation) {
      this.annotations.add(AnnotationSpec.builder(annotation).build());
      return this;
    }

    public Builder addAnnotation(Class<?> annotation) {
      return addAnnotation(ClassName.get(annotation));
    }

    public Builder addModifiers(Modifier... modifiers) {
      Collections.addAll(this.modifiers, modifiers);
      return this;
    }

    public Builder initializer(String format, Object... args) {
      return initializer(CodeBlock.of(format, args));
    }

    public Builder initializer(CodeBlock codeBlock) {
      checkState(this.initializer == null, "initializer was already set");
      this.initializer = checkNotNull(codeBlock, "codeBlock == null");
      return this;
    }

    public PropertySpec build() {
      return new PropertySpec(this);
    }
  }
}
