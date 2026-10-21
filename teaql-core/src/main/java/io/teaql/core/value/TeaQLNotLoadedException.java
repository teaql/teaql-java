package io.teaql.core.value;

import io.teaql.core.checker.ObjectLocation;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.checker.WireLocationSegment;
import java.util.List;
import java.util.Objects;

public class TeaQLNotLoadedException extends IllegalStateException {
    private final String root;
    private final ObjectLocation location;
    private final String breakPoint;
    private final JsonFieldNamingProfile profile;

    public TeaQLNotLoadedException(String root, ObjectLocation location, String breakPoint,
            JsonFieldNamingProfile profile) {
        super("TeaQLNotLoadedError: root=" + root + " access_path=" + Objects.requireNonNull(location).nativePath()
                + " break_point=" + breakPoint + " — select the missing field or relation");
        this.root = root;
        this.location = location;
        this.breakPoint = breakPoint;
        this.profile = Objects.requireNonNull(profile);
    }
    public String getRoot() { return root; }
    /** Language-native display only, never a canonical field identity. */
    public String getAccessPath() { return location.nativePath(); }
    public ObjectLocation getLocation() { return location; }
    public String getModelPath() { return location.modelPath(); }
    public String getInstancePath() { return location.instancePath(profile); }
    public String getBreakPoint() { return breakPoint; }

    /** Explicit JSON-boundary projection: do not serialize the exception/root object. */
    public WireError toWire() { return toWire(profile); }
    public WireError toWire(JsonFieldNamingProfile naming) {
        return new WireError("NOT_LOADED", location.segments(), location.instancePath(naming), breakPoint);
    }
    public record WireError(String code, List<WireLocationSegment> location, String instancePath,
            String breakPoint) { }
}
