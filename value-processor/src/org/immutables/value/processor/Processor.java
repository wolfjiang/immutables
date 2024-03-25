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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import java.io.*;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
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

    ImmutableList<Proto.Protoclass> classes = round.collectProtoclasses();
    if(!classes.isEmpty() && classes.get(0).packageOf().name().equals("com.q7link.framework.metadata.domain")){
      updateEntity();
      updateField();
      updateEnumValue();
    }
  }

  private void updateEntity(){
    File file = new File("target/generated-sources/annotations/com/q7link/framework/metadata/domain/ImmutableEntity.java");
    if(!file.exists()){
      return;
    }
    String str = readFile(file);
    str = str.replace("import java.util.ArrayList;",
        "import java.lang.reflect.InvocationTargetException;\n" +
            "import java.lang.reflect.Method;\n" +
            "import java.util.ArrayList;");
    str = str.replace("private ImmutableEntity(",
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
    str = str.replace("return title;",
        "if(titleMethod != null) {\n" +
            "      try {\n" +
            "        return (String)titleMethod.invoke(null, this, title);\n" +
            "      } catch (IllegalAccessException | InvocationTargetException e) {\n" +
            "      }\n" +
            "    }\n" +
            "    return title;");
    str = str.replace("return Optional.ofNullable(desc);",
        "if(titleMethod != null) {\n" +
            "      try {\n" +
            "        return (Optional<String>)descMethod.invoke(null, this, desc);\n" +
            "      } catch (IllegalAccessException | InvocationTargetException e) {\n" +
            "      }\n" +
            "    }\n" +
            "    return Optional.ofNullable(desc);");
    writeFile(file, str);
  }

  private void updateField(){
    File file = new File("target/generated-sources/annotations/com/q7link/framework/metadata/domain/ImmutableField.java");
    if(!file.exists()){
      return;
    }
    String str = readFile(file);
    str = str.replace("import java.util.ArrayList;",
        "import java.lang.reflect.InvocationTargetException;\n" +
            "import java.lang.reflect.Method;\n" +
            "import java.util.ArrayList;");
    str = str.replace("private ImmutableField(",
        "private static Method titleMethod = null;\n" +
            "  static {\n" +
            "    try {\n" +
            "      Class cls = Class.forName(\"com.q7link.framework.common.utils.ClassInjection\");\n" +
            "      titleMethod = cls.getMethod(\"getFieldTitle\", Field.class, String.class);\n" +
            "    } catch (ClassNotFoundException | NoSuchMethodException e) {\n" +
            "    }\n" +
            "  }\n" +
            "  private ImmutableField(");
    str = str.replace("return title;",
        "if(titleMethod != null) {\n" +
            "      try {\n" +
            "        return (String)titleMethod.invoke(null, this, title);\n" +
            "      } catch (IllegalAccessException | InvocationTargetException e) {\n" +
            "      }\n" +
            "    }\n" +
            "    return title;");
    writeFile(file, str);
  }

  private void updateEnumValue(){
    File file = new File("target/generated-sources/annotations/com/q7link/framework/metadata/domain/ImmutableEnumValueDef.java");
    if(!file.exists()){
      return;
    }
    String str = readFile(file);
    str = str.replace("import java.util.ArrayList;",
        "import java.lang.reflect.InvocationTargetException;\n" +
            "import java.lang.reflect.Method;\n" +
            "import java.util.ArrayList;");
    str = str.replace("private ImmutableEnumValueDef(",
        "private static Method titleMethod = null;\n" +
            "  static {\n" +
            "    try {\n" +
            "      Class cls = Class.forName(\"com.q7link.framework.common.utils.ClassInjection\");\n" +
            "      titleMethod = cls.getMethod(\"getEnumValueTitle\", EnumValueDef.class, String.class);\n" +
            "    } catch (ClassNotFoundException | NoSuchMethodException e) {\n" +
            "    }\n" +
            "  }\n" +
            "  private ImmutableEnumValueDef(");
    str = str.replace("return title;",
        "if(titleMethod != null) {\n" +
            "      try {\n" +
            "        return (String)titleMethod.invoke(null, this, title);\n" +
            "      } catch (IllegalAccessException | InvocationTargetException e) {\n" +
            "      }\n" +
            "    }\n" +
            "    return title;");
    writeFile(file, str);
  }

  private String readFile(File file) {
    if (!file.exists()) {
      throw new RuntimeException("file not found : " + file.getAbsolutePath());
    }
    try {
      StringBuffer stringBuffer = new StringBuffer();
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
      String content;
      while ((content = bufferedReader.readLine()) != null) {
        stringBuffer.append(content).append("\n");
      }
      bufferedReader.close();
      return stringBuffer.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeFile(File file, String content) {
    try {
      BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
      bufferedWriter.write(content);
      bufferedWriter.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
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
    super.init(new RestrictingIncrementalProcessingEnvironment(processingEnv));
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
