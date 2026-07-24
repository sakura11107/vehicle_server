package com.vehicle.server.common.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"unchecked", "rawtypes"})
public class CodeEnumConverter implements ConverterFactory<String, Enum<?>> {

    private static final Map<Class, Converter> CONVERTER_CACHE = new ConcurrentHashMap<>();

    @Override
    public <T extends Enum<?>> Converter<String, T> getConverter(Class<T> targetType) {
        return CONVERTER_CACHE.computeIfAbsent(targetType, this::createConverter);
    }

    private Converter createConverter(Class enumType) {
        Method getCodeMethod = resolveGetCodeMethod(enumType);
        if (getCodeMethod == null) {
            return new NameOnlyConverter(enumType);
        }
        return new CodeAndNameConverter(enumType, getCodeMethod);
    }

    private Method resolveGetCodeMethod(Class<?> enumType) {
        Method method = ReflectionUtils.findMethod(enumType, "getCode");
        if (method != null && method.getReturnType() == int.class) {
            return method;
        }
        return null;
    }

    private static class NameOnlyConverter<E extends Enum<E>> implements Converter<String, E> {

        private final Class<E> enumType;

        NameOnlyConverter(Class<E> enumType) {
            this.enumType = enumType;
        }

        @Override
        public E convert(String source) {
            return Enum.valueOf(enumType, source.trim());
        }
    }

    private static class CodeAndNameConverter<E extends Enum<E>> implements Converter<String, E> {

        private final Class<E> enumType;
        private final Method getCodeMethod;

        CodeAndNameConverter(Class<E> enumType, Method getCodeMethod) {
            this.enumType = enumType;
            this.getCodeMethod = getCodeMethod;
        }

        @Override
        public E convert(String source) {
            String trimmed = source.trim();
            E byCode = tryConvertByCode(trimmed);
            if (byCode != null) {
                return byCode;
            }
            return Enum.valueOf(enumType, trimmed);
        }

        private E tryConvertByCode(String codeStr) {
            Integer code = parseCode(codeStr);
            if (code == null) {
                return null;
            }
            for (E constant : enumType.getEnumConstants()) {
                try {
                    int value = (int) getCodeMethod.invoke(constant);
                    if (code == value) {
                        return constant;
                    }
                } catch (Exception ignored) {
                }
            }
            return null;
        }

        private Integer parseCode(String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
