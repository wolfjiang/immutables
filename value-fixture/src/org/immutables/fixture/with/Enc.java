package org.immutables.fixture.with;

import java.util.Optional;
import com.google.common.collect.Multimap;
import org.immutables.value.Value;

@Value.Enclosing
public interface Enc {
  // disabling builder and copy will have no effect on on-demand generation
  @Value.Immutable(builder = false, copy = false)
  interface Suppied<T extends Number> extends ImmutableEnc.WithSuppied<T> {
    String a();

    T num();

    Multimap<String, T> mm();

    Optional<T> opt();

    class Builder<T extends Number> extends ImmutableEnc.Suppied.Builder<T> {}
  }

  static void use() {
    new Suppied.Builder<Long>()
        .a("a")
        .num(0b1111L)
        .build();
  }
}