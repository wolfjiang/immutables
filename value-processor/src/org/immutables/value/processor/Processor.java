/*
   Copyright 2014 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.value.processor;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import java.io.*;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileManager.Location;
import org.immutables.generator.AbstractGenerator;
import org.immutables.generator.ForwardingFiler;
import org.immutables.generator.ForwardingProcessingEnvironment;
import org.immutables.value.processor.encode.EncodingMirror;
import org.immutables.value.processor.encode.Generator_Encodings;
import org.immutables.value.processor.meta.*;
import org.immutables.value.processor.meta.Proto.DeclaringPackage;

@SupportedAnnotationTypes({
    ImmutableMirror.QUALIFIED_NAME,
    EnclosingMirror.QUALIFIED_NAME,
    IncludeMirror.QUALIFIED_NAME,
    ModifiableMirror.QUALIFIED_NAME,
    ValueUmbrellaMirror.QUALIFIED_NAME,
    FactoryMirror.QUALIFIED_NAME,
    FConstructorMirror.QUALIFIED_NAME,
    FIncludeMirror.QUALIFIED_NAME,
    EncodingMirror.QUALIFIED_NAME,
})
public final class Processor extends AbstractGenerator {
  @Override
  protected void process() {

    Round round = ImmutableRound.builder()
        .addAllAnnotations(annotations())
        .processing(processing())
        .addAllCustomImmutableAnnotations(CustomImmutableAnnotations.annotations())
        .round(round())
        .build();

    Multimap<DeclaringPackage, ValueType> values = round.collectValues();

    invoke(new Generator_Immutables().usingValues(values).generate());
    invoke(new Generator_Modifiables().usingValues(values).generate());

    if (round.environment().hasGsonLib()) {
      invoke(new Generator_Gsons().usingValues(values).generate());
    }
    if (round.environment().hasMongoModule()) {
      invoke(new Generator_Repositories().usingValues(values).generate());
    }
    if (round.environment().hasFuncModule()) {
      invoke(new Generator_Funcs().usingValues(values).generate());
    }
    if (round.environment().hasTreesModule()) {
      invoke(new Generator_Transformers().usingValues(values).generate());
      invoke(new Generator_Visitors().usingValues(values).generate());
    }
    if (round.environment().hasAstModule()) {
      invoke(new Generator_Asts().usingValues(values).generate());
    }
    if (round.environment().hasEncodeModule()) {
      invoke(new Generator_Encodings().generate());
    }
  }

  // ---------------------------------------------------------------------------
  // Intercepting Filer: patches generated source content at write time so that
  // subsequent annotation-processing rounds can never overwrite our changes.
  // ---------------------------------------------------------------------------

  /** Replacements applied to ImmutableEntity.java */
  private static final Map<String, String> ENTITY_REPLACEMENTS = buildEntityReplacements();
  /** Replacements applied to ImmutableField.java */
  private static final Map<String, String> FIELD_REPLACEMENTS = buildFieldReplacements();
  /** Replacements applied to ImmutableEnumValueDef.java */
  private static final Map<String, String> ENUM_VALUE_REPLACEMENTS = buildEnumValueReplacements();

  private static Map<String, String> buildEntityReplacements() {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("import java.util.ArrayList;",
        "import java.lang.reflect.InvocationTargetException;\n" +
        "import java.lang.reflect.Method;\n" +
        "import java.util.ArrayList;");
    m.put("private ImmutableEntity(",
        "private static Method titleMethod = null;\n" +
        "  private static Method descMethod = null;\n" +
        "  static {\n" +
        "    try {\n" +
        "      Class cls = Class.forName(\"com.q7link.framework.common.utils.ClassInjection\");\n" +
        "      titleMethod = cls.getMethod(\"getEntityTitle\", Entity.class, String.class);\n" +
        "      descMethod = cls.getMethod(\"getEntityDesc\", Entity.class, String.class);\n" +
        "    } catch (ClassNotFoundException | NoSuchMethodException e) {\n" +
        "    }\n" +
        "  }\n" +
        "  private ImmutableEntity(");
    m.put("return title;",
        "if(titleMethod != null) {\n" +
        "      try {\n" +
        "        return (String)titleMethod.invoke(null, this, title);\n" +
        "      } catch (IllegalAccessException | InvocationTargetException e) {\n" +
        "      }\n" +
        "    }\n" +
        "    return title;");
    m.put("return Optional.ofNullable(desc);",
        "if(descMethod != null) {\n" +
        "      try {\n" +
        "        return (Optional<String>)descMethod.invoke(null, this, desc);\n" +
        "      } catch (IllegalAccessException | InvocationTargetException e) {\n" +
        "      }\n" +
        "    }\n" +
        "    return Optional.ofNullable(desc);");
    return m;
  }

  private static Map<String, String> buildFieldReplacements() {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("import java.util.ArrayList;",
        "import java.lang.reflect.InvocationTargetException;\n" +
        "import java.lang.reflect.Method;\n" +
        "import java.util.ArrayList;");
    m.put("private ImmutableField(",
        "private static Method titleMethod = null;\n" +
        "  private static Method descMethod = null;\n" +
        "  private static Method placeHolderMethod = null;\n" +
        "  static {\n" +
        "    try {\n" +
        "      Class cls = Class.forName(\"com.q7link.framework.common.utils.ClassInjection\");\n" +
        "      titleMethod = cls.getMethod(\"getFieldTitle\", Field.class, String.class);\n" +
        "      descMethod = cls.getMethod(\"getFieldDesc\", Field.class, String.class);\n" +
        "      placeHolderMethod = cls.getMethod(\"getFieldPlaceHolder\", Field.class, String.class);\n" +
        "    } catch (ClassNotFoundException | NoSuchMethodException e) {\n" +
        "    }\n" +
        "  }\n" +
        "  private ImmutableField(");
    m.put("return title;",
        "if(titleMethod != null) {\n" +
        "      try {\n" +
        "        return (String)titleMethod.invoke(null, this, title);\n" +
        "      } catch (IllegalAccessException | InvocationTargetException e) {\n" +
        "      }\n" +
        "    }\n" +
        "    return title;");
    m.put("return Optional.ofNullable(desc);",
        "if(descMethod != null) {\n" +
        "      try {\n" +
        "        return (Optional<String>)descMethod.invoke(null, this, desc);\n" +
        "      } catch (IllegalAccessException | InvocationTargetException e) {\n" +
        "      }\n" +
        "    }\n" +
        "    return Optional.ofNullable(desc);");
    m.put("return Optional.ofNullable(placeHolder);",
        "if(placeHolderMethod != null) {\n" +
        "      try {\n" +
        "        return (Optional<String>)placeHolderMethod.invoke(null, this, placeHolder);\n" +
        "      } catch (IllegalAccessException | InvocationTargetException e) {\n" +
        "      }\n" +
        "    }\n" +
        "    return Optional.ofNullable(placeHolder);");
    return m;
  }

  private static Map<String, String> buildEnumValueReplacements() {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("import java.util.ArrayList;",
        "import java.lang.reflect.InvocationTargetException;\n" +
        "import java.lang.reflect.Method;\n" +
        "import java.util.ArrayList;");
    m.put("private ImmutableEnumValueDef(",
        "private static Method titleMethod = null;\n" +
        "  static {\n" +
        "    try {\n" +
        "      Class cls = Class.forName(\"com.q7link.framework.common.utils.ClassInjection\");\n" +
        "      titleMethod = cls.getMethod(\"getEnumValueTitle\", EnumValueDef.class, String.class);\n" +
        "    } catch (ClassNotFoundException | NoSuchMethodException e) {\n" +
        "    }\n" +
        "  }\n" +
        "  private ImmutableEnumValueDef(");
    m.put("return title;",
        "if(titleMethod != null) {\n" +
        "      try {\n" +
        "        return (String)titleMethod.invoke(null, this, title);\n" +
        "      } catch (IllegalAccessException | InvocationTargetException e) {\n" +
        "      }\n" +
        "    }\n" +
        "    return title;");
    return m;
  }

  private static String applyReplacements(String source, Map<String, String> replacements) {
    for (Map.Entry<String, String> entry : replacements.entrySet()) {
      source = source.replace(entry.getKey(), entry.getValue());
    }
    return source;
  }

  /**
   * A Writer that buffers everything, applies the patch replacements, and only
   * flushes the patched content to the underlying writer on close().
   */
  private static final class PatchingWriter extends Writer {
    private final StringWriter buffer = new StringWriter();
    private final Writer delegate;
    private final Map<String, String> replacements;

    PatchingWriter(Writer delegate, Map<String, String> replacements) {
      this.delegate = delegate;
      this.replacements = replacements;
    }

    @Override public void write(char[] cbuf, int off, int len) { buffer.write(cbuf, off, len); }
    @Override public void flush() throws IOException { /* buffer until close */ }

    @Override
    public void close() throws IOException {
      String patched = applyReplacements(buffer.toString(), replacements);
      delegate.write(patched);
      delegate.close();
    }
  }

  /**
   * A JavaFileObject wrapper that returns a PatchingWriter for openWriter().
   */
  private static final class PatchingJavaFileObject implements JavaFileObject {
    private final JavaFileObject delegate;
    private final Map<String, String> replacements;

    PatchingJavaFileObject(JavaFileObject delegate, Map<String, String> replacements) {
      this.delegate = delegate;
      this.replacements = replacements;
    }

    @Override public Writer openWriter() throws IOException {
      return new PatchingWriter(delegate.openWriter(), replacements);
    }

    // --- delegation boilerplate ---
    @Override public URI toUri() { return delegate.toUri(); }
    @Override public String getName() { return delegate.getName(); }
    @Override public InputStream openInputStream() throws IOException { return delegate.openInputStream(); }
    @Override public OutputStream openOutputStream() throws IOException { return delegate.openOutputStream(); }
    @Override public Reader openReader(boolean ignoreEncodingErrors) throws IOException { return delegate.openReader(ignoreEncodingErrors); }
    @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException { return delegate.getCharContent(ignoreEncodingErrors); }
    @Override public boolean delete() { return delegate.delete(); }
    @Override public Kind getKind() { return delegate.getKind(); }
    @Override public boolean isNameCompatible(String simpleName, Kind kind) { return delegate.isNameCompatible(simpleName, kind); }
    @Override public javax.lang.model.element.NestingKind getNestingKind() { return delegate.getNestingKind(); }
    @Override public javax.lang.model.element.Modifier getAccessLevel() { return delegate.getAccessLevel(); }
    @Override public long getLastModified() { return delegate.getLastModified(); }
  }

  /**
   * A Filer that wraps createSourceFile() for the three target classes in the
   * metadata domain package and returns a PatchingJavaFileObject.
   */
  private static final class PatchingFiler extends ForwardingFiler {
    private static final String TARGET_PACKAGE = "com.q7link.framework.metadata.domain.";
    private final Filer delegate;

    PatchingFiler(Filer delegate) {
      this.delegate = delegate;
    }

    @Override protected Filer delegate() { return delegate; }

    @Override
    public JavaFileObject createSourceFile(CharSequence name, Element... originatingElements)
        throws IOException {
      JavaFileObject original = delegate.createSourceFile(name, originatingElements);
      String className = name.toString();
      if (className.equals(TARGET_PACKAGE + "ImmutableEntity")) {
        return new PatchingJavaFileObject(original, ENTITY_REPLACEMENTS);
      } else if (className.equals(TARGET_PACKAGE + "ImmutableField")) {
        return new PatchingJavaFileObject(original, FIELD_REPLACEMENTS);
      } else if (className.equals(TARGET_PACKAGE + "ImmutableEnumValueDef")) {
        return new PatchingJavaFileObject(original, ENUM_VALUE_REPLACEMENTS);
      }
      return original;
    }
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return FluentIterable.from(super.getSupportedAnnotationTypes())
        .append(CustomImmutableAnnotations.annotations())
        .toSet();
  }

  private static final String GRADLE_INCREMENTAL = "immutables.gradle.incremental";

  @Override
  public Set<String> getSupportedOptions() {
    ImmutableSet.Builder<String> options = ImmutableSet.builder();
    options.add(GRADLE_INCREMENTAL);
    if (processingEnv.getOptions().containsKey(GRADLE_INCREMENTAL)) {
      options.add("org.gradle.annotation.processing.isolating");
    }
    return options.build();
  }

  @Override
  public synchronized void init(final ProcessingEnvironment processingEnv) {
    super.init(new PatchingProcessingEnvironment(new RestrictingIncrementalProcessingEnvironment(processingEnv)));
  }

  /** Wraps the Filer so that writes to the three target classes are patched at write-time. */
  private static final class PatchingProcessingEnvironment extends ForwardingProcessingEnvironment {
    private final ProcessingEnvironment delegate;
    private Filer patchingFiler;

    PatchingProcessingEnvironment(ProcessingEnvironment delegate) {
      this.delegate = delegate;
    }

    @Override protected ProcessingEnvironment delegate() { return delegate; }

    @Override
    public Filer getFiler() {
      if (patchingFiler == null) {
        patchingFiler = new PatchingFiler(delegate.getFiler());
      }
      return patchingFiler;
    }
  }

  private final class RestrictingIncrementalProcessingEnvironment extends ForwardingProcessingEnvironment {
    private final ProcessingEnvironment processingEnv;
    boolean incrementalRestrictions;
    private Filer restrictedFiler;

    private RestrictingIncrementalProcessingEnvironment(ProcessingEnvironment processingEnv) {
      this.processingEnv = processingEnv;
      this.incrementalRestrictions = processingEnv.getOptions().containsKey(GRADLE_INCREMENTAL);
    }

    @Override
    protected ProcessingEnvironment delegate() {
      return processingEnv;
    }

    @Override
    public Filer getFiler() {
      final Filer filer = super.getFiler();
      if (incrementalRestrictions) {
        if (restrictedFiler == null) {
          restrictedFiler = new ForwardingFiler() {
            @Override
            protected Filer delegate() {
              return filer;
            }

            @Override
            public FileObject createResource(
                Location location,
                CharSequence pkg,
                CharSequence relativeName,
                Element... originatingElements)
                throws IOException {
              String message = String.format("Suppressed writing of resource %s/%s/%s (triggered by enabling -A%s)",
                  location,
                  pkg,
                  relativeName,
                  GRADLE_INCREMENTAL);
              getMessager().printMessage(Kind.MANDATORY_WARNING, message);
              throw new FileNotFoundException(message);
            }
          };
        }
        return restrictedFiler;
      }
      return filer;
    }
  }
}
