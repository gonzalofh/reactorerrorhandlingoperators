package com.gonzalofh.errorhandling.operators;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class Operators {

  Flux<Integer> pub1To3 = Flux.fromStream(Stream.of(1, 2, 3));
  Flux<Error> pubRuntimeExc = Flux.error(new RuntimeException());
  Flux<Error> pubIllegalArgExc = Flux.error(new IllegalArgumentException());
  Flux<Integer> pub6to8 = Flux.fromStream(Stream.of(6, 7, 8));

  @Test
  public void publisherIsCompleteAfterFirstError() {

    var pub = Flux.merge(pub1To3, pubRuntimeExc, pubIllegalArgExc, pub6to8);

    /* Events 1, 2, 3 will happen. Then error signal with RuntimeException will close the publisher */
    StepVerifier.create(pub)
        .expectNext(1, 2, 3)
        .expectError(RuntimeException.class)
        .verify();

  }

  /**
   * https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#doOnError-java.lang.Class-java.util.function.Consumer-
   */
  @Test
  public void doOnError() {

    var pub = Flux.merge(pub1To3, pubRuntimeExc, pubIllegalArgExc, pub6to8)
        .doOnError(System.out::println);

    /* Events 1, 2, 3 will happen. Then error signal with RuntimeException will close the publisher.
       doOnError will only apply a side effect to the error event,
       but it won't have any effect on the signal propagating through the publisher */
    StepVerifier.create(pub)
        .expectNext(1, 2, 3)
        .expectError(RuntimeException.class)
        .verify();

  }

  /**
   * https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#onErrorComplete--
   */
  @Test
  public void onErrorComplete() {

    var pub = Flux.merge(pub1To3, pubRuntimeExc, pubIllegalArgExc, pub6to8)
        .onErrorComplete();

    /* Events 1, 2, 3 will happen.
       Then, onErrorComplete will replace the onError signal with an onComplete signal.*/
    StepVerifier.create(pub)
        .expectNext(1, 2, 3)
        .expectComplete()
        .verify();

  }

  /**
   * https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#onErrorContinue-java.util.function.BiConsumer-
   */
  @Test
  public void onErrorContinue() {

    var pub = Flux.merge(pub1To3, pubRuntimeExc, pubIllegalArgExc, pub6to8)
        .onErrorContinue((throwable, o) -> {
          System.out.println("error " + throwable);
          System.out.println("object " + o);
        });

    /* Events 1, 2, 3 will happen.
       Then, onErrorContinue will drop the errors and continue with the subsequent elements.
       onErrorContinue operates on upstream, not downstream operators. */
    StepVerifier.create(pub)
        .expectNext(1, 2, 3, 6, 7, 8)
        .expectComplete()
        .verify();

  }

  /**
   * https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#onErrorStop--
   */
  @Test
  public void onErrorStop() {

    var pub = Flux.merge(pub1To3, pubRuntimeExc, pubIllegalArgExc, pub6to8)
        .onErrorStop()
        .onErrorContinue((throwable, o) -> {
          System.out.println("error " + throwable);
          System.out.println("object " + o);
        });

    /* Events 1, 2, 3 will happen.
       Then the RuntimeException will be thrown.
       When onErrorStop() is applied, it reverts to the default 'STOP' mode where errors are terminal events upstream.
       It basically cancels out whatever onErrorContinue you may have downstream */
    StepVerifier.create(pub)
        .expectNext(1, 2, 3)
        .expectError(RuntimeException.class)
        .verify();

  }

  /**
   * https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#onErrorMap-java.lang.Class-java.util.function.Function-
   */
  @Test
  public void onErrorMap() {

    var pub = Flux.merge(pub1To3, pubRuntimeExc, pubIllegalArgExc, pub6to8)
        .onErrorMap(e -> (e instanceof RuntimeException) ? new IllegalStateException() : e)
        .onErrorMap(IllegalArgumentException.class, e ->  new NoSuchFieldException()); // won't affect

    /* Events 1, 2, 3 will happen.
       Then the RuntimeException will be mapped into a IllegalStateException
       And the IllegalStateException will be thrown.*/
    StepVerifier.create(pub)
        .expectNext(1, 2, 3)
        .expectError(IllegalStateException.class)
        .verify();

  }

  /**
   * https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#onErrorResume-java.lang.Class-java.util.function.Function-
   */
  @Test
  public void onErrorResume() {

    var pub = Flux.merge(pub1To3, pubRuntimeExc, pubIllegalArgExc, pub6to8)
        .onErrorResume(RuntimeException.class, e -> Flux.fromStream(Stream.of(4, 5)))
        .onErrorResume(e -> Flux.fromStream(Stream.of(10, 11, 12))); // won't affect

    /* Events 1, 2, 3 will happen.
       Then events 4, 5 will happen as a result of the Flux.fromStream(Stream.of(4, 5)))
       being a fallback async operation when running into an error. */
    StepVerifier.create(pub)
        .expectNext(1, 2, 3, 4, 5)
        .expectComplete()
        .verify();

  }

  /**
   * https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#onErrorReturn-java.lang.Class-T-
   */
  @Test
  public void onErrorReturn() {

    var pub = Flux.merge(pub1To3, pubRuntimeExc, pubIllegalArgExc, pub6to8)
        .onErrorReturn(RuntimeException.class, 99)
        .onErrorReturn(-1); // won't affect

    /* Events 1, 2, 3 will happen.
       Then event 99 will happen as a result of it being a fallback item when running into an error. */
    StepVerifier.create(pub)
        .expectNext(1, 2, 3, 99)
        .expectComplete()
        .verify();

  }

}
