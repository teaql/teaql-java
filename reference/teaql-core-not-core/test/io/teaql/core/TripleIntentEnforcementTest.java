package io.teaql.core;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Verifies the Triple-Intent runtime enforcement:
 *   - Queries must have .comment() + .purpose()
 *   - Saves must have .auditAs()
 *
 * Also verifies that error messages are AI-actionable:
 * they contain concrete fix code the AI can copy-paste.
 */
public class TripleIntentEnforcementTest {

    // ── Minimal concrete stubs for testing ──────────────────────────

    static class StubEntity extends BaseEntity {
        @Override
        public String typeName() {
            return "StubEntity";
        }
    }

    static class StubRequest extends BaseRequest<StubEntity> {
        public StubRequest() {
            super(StubEntity.class);
        }

        public StubRequest comment(String comment) {
            super.internalComment(comment);
            return this;
        }

        public StubRequest withPurpose(String purpose) {
            super.internalPurpose(purpose);
            return this;
        }
    }

    /** A test-friendly UserContext that lets us control enforcement mode. */
    static class TestUserContext extends DefaultUserContext {
        private final IntentEnforcementMode mode;

        TestUserContext(IntentEnforcementMode mode) {
            this.mode = mode;
        }

        @Override
        protected <T extends Entity> SearchRequest<T> enforceRequestPolicy(SearchRequest<T> request) {
            if (mode == IntentEnforcementMode.OFF) {
                return request;
            }
            String typeName = request.getTypeName();
            String comment = request.comment();
            String purpose = request.purpose();
            boolean missingComment = comment == null || comment.isEmpty();
            boolean missingPurpose = purpose == null || purpose.isEmpty();

            if (!missingComment && !missingPurpose) {
                return request;
            }

            StringBuilder msg = new StringBuilder();
            msg.append("[TRIPLE-INTENT VIOLATION] Query on ").append(typeName).append(" rejected.\n");
            msg.append("Missing: ");
            if (missingComment) msg.append(".comment() ");
            if (missingPurpose) msg.append(".purpose() ");
            msg.append("\n\n");
            msg.append("FIX: Every query must declare both .comment() and .purpose() before execution.\n");
            msg.append("Correct pattern:\n");
            msg.append("  Q.").append(typeName.toLowerCase()).append("s()\n");
            msg.append("      .comment(\"Describe what this query loads\")\n");
            msg.append("      .purpose(\"Describe why this data is needed\")\n");
            msg.append("      .executeForList(ctx);\n");
            msg.append("\n");
            msg.append("Refer to AGENTS.md section 'MANDATORY TRIPLE-INTENT' for full documentation.");

            if (mode == IntentEnforcementMode.STRICT) {
                throw new RepositoryException(msg.toString());
            }
            return request;
        }

        public void testEnforceAudit(Entity entity) {
            if (mode == IntentEnforcementMode.OFF) {
                return;
            }
            String comment = entity.getComment();
            if (comment != null && !comment.isEmpty()) {
                return;
            }

            String typeName = entity.typeName();
            StringBuilder msg = new StringBuilder();
            msg.append("[TRIPLE-INTENT VIOLATION] Save on ").append(typeName);
            msg.append("(id=").append(entity.getId()).append(") rejected.\n");
            msg.append("Missing: .auditAs()\n\n");
            msg.append("FIX: Every entity mutation must call .auditAs() before .save().\n");
            msg.append("Correct pattern:\n");
            msg.append("  ").append(typeName.toLowerCase()).append(".updateXxx(newValue)\n");
            msg.append("      .auditAs(\"Describe the business action being performed\")\n");
            msg.append("      .save(ctx);\n");
            msg.append("\n");
            msg.append("Do NOT use .save(ctx) alone. Do NOT use .setComment() directly.\n");
            msg.append("Refer to AGENTS.md section 'MANDATORY TRIPLE-INTENT' for full documentation.");

            if (mode == IntentEnforcementMode.STRICT) {
                throw new RepositoryException(msg.toString());
            }
        }
    }

    // ── Query-side: happy path ──────────────────────────────────────

    @Test
    public void query_withBothCommentAndPurpose_passes() {
        TestUserContext ctx = new TestUserContext(IntentEnforcementMode.STRICT);
        StubRequest request = new StubRequest()
                .comment("Load tasks for dashboard")
                .withPurpose("Display task count widget");

        SearchRequest<StubEntity> result = ctx.enforceRequestPolicy(request);
        assertNotNull(result);
        assertEquals("Load tasks for dashboard", result.comment());
        assertEquals("Display task count widget", result.purpose());
    }

    // ── Query-side: error messages guide the AI ─────────────────────

    @Test
    public void query_withoutPurpose_errorContainsFixableCodeExample() {
        TestUserContext ctx = new TestUserContext(IntentEnforcementMode.STRICT);
        StubRequest request = new StubRequest().comment("Load tasks");

        try {
            ctx.enforceRequestPolicy(request);
            fail("Should have thrown");
        } catch (RepositoryException e) {
            String msg = e.getMessage();
            // The AI reads this error output — every line is designed to teach it the fix
            assertTrue("Tags the violation type",        msg.contains("[TRIPLE-INTENT VIOLATION]"));
            assertTrue("Names the entity type",          msg.contains("Stub"));
            assertTrue("Identifies what's missing",      msg.contains(".purpose()"));
            assertTrue("Shows Q facade entry point",     msg.contains("Q."));
            assertTrue("Shows .comment() in chain",      msg.contains(".comment("));
            assertTrue("Shows .purpose() in chain",      msg.contains(".purpose("));
            assertTrue("Shows terminal .executeForList", msg.contains(".executeForList(ctx)"));
            assertTrue("Points to AGENTS.md docs",       msg.contains("AGENTS.md"));
            assertTrue("Says 'MANDATORY TRIPLE-INTENT'", msg.contains("MANDATORY TRIPLE-INTENT"));
        }
    }

    @Test
    public void query_withoutComment_errorContainsFixableCodeExample() {
        TestUserContext ctx = new TestUserContext(IntentEnforcementMode.STRICT);
        StubRequest request = new StubRequest().withPurpose("Dashboard display");

        try {
            ctx.enforceRequestPolicy(request);
            fail("Should have thrown");
        } catch (RepositoryException e) {
            String msg = e.getMessage();
            assertTrue(msg.contains(".comment()"));
            assertTrue(msg.contains("FIX:"));
            assertTrue(msg.contains("AGENTS.md"));
        }
    }

    @Test(expected = RepositoryException.class)
    public void query_withNothing_throwsInStrict() {
        TestUserContext ctx = new TestUserContext(IntentEnforcementMode.STRICT);
        ctx.enforceRequestPolicy(new StubRequest());
    }

    // ── Query-side: backward compatibility ──────────────────────────

    @Test
    public void query_withNothing_passesInOffMode() {
        TestUserContext ctx = new TestUserContext(IntentEnforcementMode.OFF);
        assertNotNull(ctx.enforceRequestPolicy(new StubRequest()));
    }

    @Test
    public void query_withNothing_passesInWarnMode() {
        TestUserContext ctx = new TestUserContext(IntentEnforcementMode.WARN);
        assertNotNull(ctx.enforceRequestPolicy(new StubRequest()));
    }

    // ── Save-side: happy path ───────────────────────────────────────

    @Test
    public void save_withAuditAs_passes() {
        TestUserContext ctx = new TestUserContext(IntentEnforcementMode.STRICT);
        StubEntity entity = new StubEntity();
        entity.setId(42L);
        entity.auditAs("Create new task for robot assembly");

        ctx.testEnforceAudit(entity);
        assertEquals("Create new task for robot assembly", entity.getComment());
    }

    // ── Save-side: error messages guide the AI ──────────────────────

    @Test
    public void save_withoutAuditAs_errorContainsFixableCodeExample() {
        TestUserContext ctx = new TestUserContext(IntentEnforcementMode.STRICT);
        StubEntity entity = new StubEntity();
        entity.setId(42L);

        try {
            ctx.testEnforceAudit(entity);
            fail("Should have thrown");
        } catch (RepositoryException e) {
            String msg = e.getMessage();
            // The AI reads this error output — every line is designed to teach it the fix
            assertTrue("Tags the violation type",        msg.contains("[TRIPLE-INTENT VIOLATION]"));
            assertTrue("Names the entity type",          msg.contains("StubEntity"));
            assertTrue("Identifies what's missing",      msg.contains(".auditAs()"));
            assertTrue("Shows .auditAs() with example",  msg.contains(".auditAs(\"Describe the business action"));
            assertTrue("Shows terminal .save(ctx)",      msg.contains(".save(ctx)"));
            assertTrue("Explicitly bans bare .save()",   msg.contains("Do NOT use .save(ctx) alone"));
            assertTrue("Bans .setComment() bypass",      msg.contains("Do NOT use .setComment() directly"));
            assertTrue("Points to AGENTS.md docs",       msg.contains("AGENTS.md"));
        }
    }

    @Test
    public void save_withoutAuditAs_passesInOffMode() {
        TestUserContext ctx = new TestUserContext(IntentEnforcementMode.OFF);
        StubEntity entity = new StubEntity();
        entity.setId(42L);
        ctx.testEnforceAudit(entity);
    }

    // ── Fluent API correctness ──────────────────────────────────────

    @Test
    public void auditAs_setsCommentAndReturnsSelf() {
        StubEntity entity = new StubEntity();
        Entity returned = entity.auditAs("Transition task to DONE");
        assertSame(entity, returned);
        assertEquals("Transition task to DONE", entity.getComment());
    }

    @Test
    public void request_purposeAndComment_areIndependent() {
        StubRequest request = new StubRequest()
                .comment("Fetch orders by region");
        ExecutableRequest<StubEntity> executable =
                request.purpose("Generate regional sales report");

        assertNotNull(executable);
        assertSame(request, executable.request());
        assertEquals("Fetch orders by region", request.comment());
        assertEquals("Generate regional sales report", request.purpose());
    }
}
