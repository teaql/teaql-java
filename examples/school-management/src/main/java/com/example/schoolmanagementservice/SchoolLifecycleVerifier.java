package com.example.schoolmanagementservice;

import com.example.schoolmanagementservice.platform.Platform;
import com.example.schoolmanagementservice.school.School;
import io.teaql.core.UserContext;
import io.teaql.core.checker.CheckException;
import io.teaql.runtime.TeaQLRuntime;
import java.time.LocalDate;
import java.util.Locale;

/** Application-owned, repeatable Q/E/Checker/mutation smoke test. */
final class SchoolLifecycleVerifier {
  private SchoolLifecycleVerifier() {}

  static void verify(TeaQLRuntime runtime) {
    UserContext context = new CustomUserContext(runtime);
    Platform platform = Q.platforms()
        .withIdIs(1L)
        .comment("what: load the seeded platform for the lifecycle probe")
        .purpose("why: attach the School to the domain root")
        .executeForOne(context);
    require(platform != null, "The seeded Platform is missing");

    // Checker must reject the missing required name before a database NOT NULL error.
    UserContext invalidContext = new CustomUserContext(runtime);
    School invalid = Q.schools()
        .comment("what: prepare an incomplete School")
        .purpose("why: verify pre-SQL checker rejection")
        .newEntity(invalidContext);
    invalid.updatePlatform(platform);
    invalid.updateSchoolTypeToPrimary();
    invalid.updateAddress("Lifecycle Probe Road");
    invalid.updateEstablishedDate(LocalDate.of(2020, 1, 1));
    invalid.updateStudentCapacity(10);
    invalid.updateActive(true);
    boolean checkerRejected = false;
    try {
      invalid.auditAs("Reject an incomplete School in the lifecycle probe").save(invalidContext);
    } catch (CheckException exception) {
      String message = String.valueOf(exception.getMessage());
      require(message.toLowerCase(Locale.ROOT).contains("name"),
          "Checker did not identify the missing School name: " + message);
      checkerRejected = true;
    }
    require(checkerRejected, "Checker accepted a School without its required name");

    UserContext createContext = new CustomUserContext(runtime);
    School created = Q.schools()
        .comment("what: initialize the School lifecycle probe")
        .purpose("why: verify audited graph mutation")
        .newEntity(createContext);
    created.updatePlatform(platform);
    created.updateSchoolTypeToPrimary();
    created.updateName("Lifecycle Probe School");
    created.updateAddress("Lifecycle Probe Road");
    created.updateEstablishedDate(LocalDate.of(2020, 1, 1));
    created.updateStudentCapacity(10);
    created.updateActive(true);
    created.auditAs("Create the School lifecycle probe").save(createContext);
    Long id = created.getId();
    require(id != null, "Audited create did not assign a School ID");

    UserContext readContext = new CustomUserContext(runtime);
    School loaded = Q.schools()
        .withIdIs(id)
        .selectPlatformWith(Q.platformsWithMinimalFields().selectName())
        .selectSchoolTypeWith(Q.schoolTypesWithMinimalFields().selectCode())
        .comment("what: load the complete School lifecycle probe")
        .purpose("why: evaluate loaded E expressions and update a complete entity")
        .executeForOne(readContext);
    require(loaded != null && "Lifecycle Probe School".equals(E.school(loaded).getName().eval()),
        "Created School was not readable through Q and E");
    require("PRIMARY".equals(E.school(loaded).getSchoolType().eval().getCode()),
        "E could not traverse the loaded SchoolType relation");

    UserContext updateContext = new CustomUserContext(runtime);
    School forUpdate = Q.schools()
        .withIdIs(id)
        .comment("what: load all School fields without partial relation projections")
        .purpose("why: give Checker a complete entity for the audited update")
        .executeForOne(updateContext);
    require(forUpdate != null, "School was lost before its update");
    forUpdate.updateName("Lifecycle Probe Academy");
    forUpdate.auditAs("Rename the School lifecycle probe").save(updateContext);

    UserContext deleteContext = new CustomUserContext(runtime);
    School renamed = Q.schools()
        .withIdIs(id)
        .comment("what: reload the renamed School lifecycle probe")
        .purpose("why: verify the update and preserve the deletion version")
        .executeForOne(deleteContext);
    require(renamed != null && "Lifecycle Probe Academy".equals(renamed.getName()),
        "Audited School update did not persist");
    renamed.markForDeletion()
        .auditAs("Delete the School lifecycle probe")
        .save(deleteContext);

    UserContext verifyContext = new CustomUserContext(runtime);
    School visible = Q.schools()
        .withIdIs(id)
        .comment("what: check normal visibility after soft deletion")
        .purpose("why: verify mark-for-deletion plus save")
        .executeForOne(verifyContext);
    require(visible == null, "A deleted School remains visible in a normal query");
    School deleted = Q.schools()
        .deletedRowsOnly()
        .withIdIs(id)
        .comment("what: inspect the deleted School lifecycle probe")
        .purpose("why: verify the row remains with a negative version")
        .executeForOne(verifyContext);
    require(deleted != null && deleted.getVersion() < 0,
        "Soft-deleted School is missing or has a nonnegative version");
    System.out.println("PASS Java School Q/E/Checker/audited create-update-delete lifecycle");
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }
}
