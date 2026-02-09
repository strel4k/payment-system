//package com.example.personservice.util;
//
//import java.lang.reflect.Method;
//import java.util.Map;
//import java.util.UUID;
//
//public final class DtoAccess {
//
//    private DtoAccess() {}
//
//    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS = Map.of(
//            boolean.class, Boolean.class,
//            int.class, Integer.class,
//            long.class, Long.class,
//            double.class, Double.class,
//            float.class, Float.class,
//            short.class, Short.class,
//            byte.class, Byte.class,
//            char.class, Character.class
//    );
//
//    public static String getString(Object dto, String... getterNames) {
//        Object v = get(dto, getterNames);
//        return (v instanceof String s) ? s : null;
//    }
//
//    public static UUID getUuid(Object dto, String... getterNames) {
//        Object v = get(dto, getterNames);
//        return (v instanceof UUID u) ? u : null;
//    }
//
//    public static Object get(Object dto, String... getterNames) {
//        if (dto == null) return null;
//        for (String name : getterNames) {
//            Object v = invokeNoArgs(dto, name);
//            if (v != null) return v;
//        }
//        return null;
//    }
//
//    public static void set(Object dto, Object value, String... setterNames) {
//        if (dto == null || value == null) return;
//        for (String name : setterNames) {
//            if (tryInvokeSetter(dto, name, value)) return;
//        }
//    }
//
//    private static Object invokeNoArgs(Object target, String methodName) {
//        try {
//            Method m = target.getClass().getMethod(methodName);
//            return m.invoke(target);
//        } catch (Exception ignored) {
//            // пробуем ещё вариант: иногда генераторы делают public field без getter — не трогаем, просто null
//            return null;
//        }
//    }
//
//    private static boolean tryInvokeSetter(Object target, String methodName, Object value) {
//        Method[] methods = target.getClass().getMethods();
//        for (Method m : methods) {
//            if (!m.getName().equals(methodName)) continue;
//            if (m.getParameterCount() != 1) continue;
//
//            Class<?> p = m.getParameterTypes()[0];
//
//            // 1) обычная совместимость типов
//            if (p.isAssignableFrom(value.getClass())) {
//                try {
//                    m.invoke(target, value);
//                    return true;
//                } catch (Exception ignored) {
//                    return false;
//                }
//            }
//
//            // 2) примитивы: boolean <- Boolean и т.д.
//            if (p.isPrimitive()) {
//                Class<?> wrapper = PRIMITIVE_WRAPPERS.get(p);
//                if (wrapper != null && wrapper.isAssignableFrom(value.getClass())) {
//                    try {
//                        m.invoke(target, value);
//                        return true;
//                    } catch (Exception ignored) {
//                        return false;
//                    }
//                }
//            }
//        }
//        return false;
//    }
//}