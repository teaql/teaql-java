package io.teaql.core.checker;

import java.util.ArrayList;
import java.util.List;

public class ObjectLocation {
    private ObjectLocation parent;

    public ObjectLocation(ObjectLocation pParent) {
        parent = pParent;
    }

    public static ObjectLocation hashRoot(String memberName) {
        return new HashLocation(null, memberName);
    }

    public static ObjectLocation arrayRoot(int index) {
        return new ArrayLocation(null, index);
    }

    public ObjectLocation getParent() {
        return parent;
    }

    public ObjectLocation member(String memberName) {
        return new HashLocation(this, memberName);
    }

    public ObjectLocation element(int index) {
        return new ArrayLocation(this, index);
    }

    public int getLevel() {
        if (getParent() == null) {
            return 1;
        }
        return getParent().getLevel() + 1;
    }

    public boolean isFirstLevel() {
        return getLevel() == 1;
    }

    public boolean isSecondLevel() {
        return getLevel() == 2;
    }

    public boolean isThirdLevel() {
        return getLevel() == 3;
    }

    /** Canonical casing-neutral path using KSML property names. */
    public String modelPath() {
        return render(false, false);
    }

    /** Java diagnostic path using lower-camel property names. */
    public String nativePath() {
        return render(true, false);
    }

    /** RFC 6901 JSON pointer using TeaQL's default lower-camel wire policy. */
    public String instancePath() {
        return instancePath(JsonFieldNamingProfile.CAMEL_CASE);
    }

    /** RFC 6901 JSON pointer rendered with the model-selected wire profile. */
    public String instancePath(JsonFieldNamingProfile profile) {
        return render(profile, true);
    }

    public List<WireLocationSegment> segments() {
        List<WireLocationSegment> result = new ArrayList<>();
        for (ObjectLocation current = this; current != null; current = current.getParent()) {
            if (current instanceof HashLocation hash) {
                result.add(0, WireLocationSegment.property(hash.getMember()));
            } else if (current instanceof ArrayLocation array) {
                result.add(0, WireLocationSegment.index(array.getIndex()));
            }
        }
        return List.copyOf(result);
    }

    private String render(boolean lowerCamel, boolean pointer) {
        return render(lowerCamel ? JsonFieldNamingProfile.CAMEL_CASE : null, pointer);
    }

    private String render(JsonFieldNamingProfile profile, boolean pointer) {
        List<ObjectLocation> locations = new ArrayList<>();
        for (ObjectLocation current = this; current != null; current = current.getParent()) {
            locations.add(0, current);
        }
        StringBuilder result = new StringBuilder();
        for (ObjectLocation location : locations) {
            if (location instanceof HashLocation hash) {
                String member = profile == null ? hash.getMember() : profile.render(hash.getMember());
                if (pointer) {
                    result.append('/').append(escapePointer(member));
                } else {
                    if (!result.isEmpty()) result.append('.');
                    result.append(member);
                }
            } else if (location instanceof ArrayLocation array) {
                if (pointer) result.append('/').append(array.getIndex());
                else result.append('[').append(array.getIndex()).append(']');
            }
        }
        return result.toString();
    }

    private static String lowerCamel(String name) {
        String[] parts = name.split("_", -1);
        StringBuilder result = new StringBuilder(parts.length == 0 ? "" : parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            result.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return result.toString();
    }

    private static String escapePointer(String value) {
        return value.replace("~", "~0").replace("/", "~1");
    }
}
