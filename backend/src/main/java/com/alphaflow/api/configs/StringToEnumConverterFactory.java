package com.alphaflow.api.configs;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

@SuppressWarnings("rawtypes")
public class StringToEnumConverterFactory implements ConverterFactory<String, Enum> {

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
    Class<?> enumType = targetType;
    while (enumType != null && !enumType.isEnum()) {
      enumType = enumType.getSuperclass();
    }
    if (enumType != null) {
      MethodHandle fromHandle = findFromHandle(enumType);
      Class<T> resolvedEnumType = (Class<T>) enumType;
      return source -> convert(resolvedEnumType, fromHandle, source);
    } else {
      throw new IllegalArgumentException(
          "The target type " + targetType.getName() + " is not an enum");
    }
  }

  private MethodHandle findFromHandle(Class<?> enumType) {
    try {
      return MethodHandles.publicLookup()
          .findStatic(enumType, "from", MethodType.methodType(enumType, String.class));
    } catch (NoSuchMethodException | IllegalAccessException exception) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends Enum> T convert(Class<T> enumType, MethodHandle fromHandle, String source) {
    if (source != null && !source.trim().isEmpty()) {
      String trimmed = source.trim();
      if (fromHandle != null) {
        try {
          return (T) fromHandle.invoke(trimmed);
        } catch (Throwable throwable) {
          throw new IllegalArgumentException(throwable.getMessage(), throwable);
        }
      } else {
        return (T) Enum.valueOf((Class) enumType, trimmed.toUpperCase());
      }
    } else {
      return null;
    }
  }
}
