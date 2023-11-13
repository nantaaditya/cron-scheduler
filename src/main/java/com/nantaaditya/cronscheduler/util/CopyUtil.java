package com.nantaaditya.cronscheduler.util;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CopyUtil {

  public static <S, T> T copy(S source, T target) {
    if (source == null) {
      return null;
    }

    BeanUtils.copyProperties(source, target);
    return target;
  }

  public static <S, T> T copy(S source, Supplier<T> tSupplier) {
    if (source == null) {
      return null;
    }

    T result = tSupplier.get();
    BeanUtils.copyProperties(source, result);
    return result;
  }

  public static <S, T> List<T> copy(List<S> source, Supplier<T> target) {
    if (source == null || source.isEmpty()) {
      return Collections.emptyList();
    }

    return source.stream()
        .map(item -> copy(item, target))
        .toList();
  }

  public static <S, T> List<T> copy(List<S> source, Supplier<T> target, BiFunction<S, T, T> biFunction) {
    if (source == null || source.isEmpty()) {
      return Collections.emptyList();
    }

    return source.stream()
        .map(item -> {
          T result = copy(item, target);
          return biFunction.apply(item, result);
        })
        .toList();
  }
}
