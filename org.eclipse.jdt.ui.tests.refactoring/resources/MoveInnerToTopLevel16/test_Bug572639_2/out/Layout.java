package p;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public interface Layout /*permits Layout.PrimitiveLayout, Layout.ListLayout, Layout.StructLayout*/ {
    default boolean isPrimitive() {
      return this instanceof Layout.PrimitiveLayout;
    }
    default boolean isList() {
      return this instanceof Layout.ListLayout;
    }
    default boolean isStruct() {
      return this instanceof Layout.StructLayout;
    }
    boolean nullable();
    default Map<String, Layout> fields() {
      return Map.of();
    }
    default Layout field(String name) {
      throw new IllegalArgumentException("unknown field " + name);
    }
    default Layout element() {
      throw new IllegalArgumentException("no element");
    }

    static Layout.PrimitiveLayout u1(boolean nullable) {
      return new PrimitiveLayout(nullable, boolean.class);
    }
    static Layout.PrimitiveLayout byte8(boolean nullable) {
      return new PrimitiveLayout(nullable, byte.class);
    }
    static Layout.PrimitiveLayout short16(boolean nullable) {
      return new PrimitiveLayout(nullable, short.class);
    }
    static Layout.PrimitiveLayout char16(boolean nullable) {
      return new PrimitiveLayout(nullable, char.class);
    }
    static Layout.PrimitiveLayout int32(boolean nullable) {
      return new PrimitiveLayout(nullable, int.class);
    }
    static Layout.PrimitiveLayout float32(boolean nullable) {
      return new PrimitiveLayout(nullable, float.class);
    }
    static Layout.PrimitiveLayout double64(boolean nullable) {
      return new PrimitiveLayout(nullable, double.class);
    }
    static Layout.PrimitiveLayout long64(boolean nullable) {
      return new PrimitiveLayout(nullable, long.class);
    }

    static Layout.ListLayout list(boolean nullable, Layout layout) {
      return new ListLayout(nullable, layout);
    }
    static Layout.ListLayout string(boolean nullable) {
      return list(true, char16(nullable));
    }

    static Layout.Field field(String name, Layout layout) {
      return new Field(name, layout);
    }
    static Layout.StructLayout struct(boolean nullable, Layout.Field... fields) {
      return new StructLayout(nullable, Arrays.stream(fields).collect(toMap(Field::name, Field::layout, (_1, _2) -> null, LinkedHashMap::new)));
    }

    private static String toString(String space, Layout layout) {
      if (layout instanceof Layout.PrimitiveLayout primitiveLayout) {
        return primitiveLayout.toString();
      }
      if (layout instanceof Layout.ListLayout listLayout) {
        if (listLayout.element instanceof Layout.PrimitiveLayout elementLayout && elementLayout.type == char.class) {
          return "string(" + elementLayout.nullable + ")";
        }
        return "list(" + listLayout.nullable + ", " + toString(space, listLayout.element);
      }
      if (layout instanceof Layout.StructLayout structLayout) {
        if (structLayout.fields.isEmpty()) {
          return "struct(" + structLayout.nullable + ")";
        }
        var newSpace = space + "   ";
        return structLayout.fields.entrySet().stream()
            .map(e -> newSpace + "field(\"" + e.getKey() + "\", " + toString(newSpace, e.getValue()) + ")")
            .collect(joining(",\n", "struct(" + structLayout.nullable + ",\n", "\n" + space + ")"));
      }
      throw new AssertionError();
    }

    record PrimitiveLayout(boolean nullable, Class<?> type) implements Layout {
      public PrimitiveLayout {
        requireNonNull(type);
        if (!type.isPrimitive()) {
          throw new IllegalArgumentException("only primitive types are allowed");
        }
      }

      @Override
      public String toString() {
        var name = switch(type.getName()) {
          case "boolean" -> "u1";
          case "byte" -> "byte8";
          case "short" -> "short16";
          case "char" -> "char16";
          case "int" -> "int32";
          case "float" -> "float32";
          case "long" -> "long64";
          case "double" -> "double64";
          default -> throw new AssertionError();
        };
        return name + "(" + nullable + ")";
      }
    }
    record ListLayout(boolean nullable, Layout element) implements Layout {
      public ListLayout {
        requireNonNull(element);
      }

      @Override
      public String toString() {
        return Layout.toString("", this);
      }
    }
    record StructLayout(boolean nullable, Map<String, Layout> fields) implements Layout {
      public StructLayout {
        fields = new LinkedHashMap<>(fields);
      }
      
      @Override
      public Layout field(String name) {
        var layout = fields.get(name);
        if (layout == null) {
          throw new IllegalArgumentException("unknown field " + name);
        }
        return layout;
      }

      @Override
      public Map<String, Layout> fields() {
        return unmodifiableMap(fields);
      }

      @Override
      public String toString() {
        return Layout.toString("", this);
      }
    }
    record Field(String name, Layout layout) {
      public Field {
        requireNonNull(name);
        requireNonNull(layout);
      }

      @Override
      public String toString() {
        return "field(" + name + ", " + layout + ")";
      }
    }
  }