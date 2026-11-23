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
    private final ExpressionTrace trace;

    public TeaQLNotLoadedException(String root, ObjectLocation location, String breakPoint,
            JsonFieldNamingProfile profile) {
        this(root, location, breakPoint, profile, null);
    }

    private TeaQLNotLoadedException(String root, ObjectLocation location, String breakPoint,
            JsonFieldNamingProfile profile, ExpressionTrace trace) {
        super("TeaQLNotLoadedError: root=" + root + " access_path=" + Objects.requireNonNull(location).nativePath()
                + " break_point=" + breakPoint + " — select the missing field or relation");
        this.root = root;
        this.location = location;
        this.breakPoint = breakPoint;
        this.profile = Objects.requireNonNull(profile);
        this.trace = trace;
    }

    /** Preserve the first failure while outer lazy expressions supply the complete attempt. */
    public TeaQLNotLoadedException withAttempt(ExpressionTrace attempt) {
        return new TeaQLNotLoadedException(root, location, breakPoint, profile, Objects.requireNonNull(attempt));
    }
    public String getRoot() { return root; }
    /** Language-native display only, never a canonical field identity. */
    public String getAccessPath() { return location.nativePath(); }
    public ObjectLocation getLocation() { return location; }
    public String getModelPath() { return location.modelPath(); }
    public String getInstancePath() { return location.instancePath(profile); }
    public String getBreakPoint() { return breakPoint; }
    public String getOriginalExpression() { return trace == null ? null : trace.render(null); }
    public String getExpressionWithBrokenPoint() { return trace == null ? null : trace.render(location); }
    @Override public String getMessage() {
        return super.getMessage() + (trace == null ? "" : " expression=" + getExpressionWithBrokenPoint());
    }

    /** Explicit JSON-boundary projection: do not serialize the exception/root object. */
    public WireError toWire() { return toWire(profile); }
    public WireError toWire(JsonFieldNamingProfile naming) {
        ObjectLocation attempted = trace == null || trace.location() == null ? location : trace.location();
        return new WireError("NOT_LOADED", location.segments(), location.instancePath(naming), breakPoint,
                attempted.segments(), attempted.instancePath(naming), location.segments().size() - 1,
                getOriginalExpression(), getExpressionWithBrokenPoint(),
                new PreloadGuidance("preload", location.segments()));
    }
    public record WireError(String code, List<WireLocationSegment> location, String instancePath,
            String breakPoint, List<WireLocationSegment> attemptedLocation, String attemptedInstancePath,
            int breakPointIndex, String originalExpression, String expressionWithBrokenPoint,
            PreloadGuidance suggestedFix) { }
    public record PreloadGuidance(String action, List<WireLocationSegment> location) { }
}
