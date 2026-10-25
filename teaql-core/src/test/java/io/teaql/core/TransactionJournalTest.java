package io.teaql.core;

import java.util.*;
import org.junit.Test;
import static io.teaql.core.TransactionCompletion.Outcome.*;
import static org.junit.Assert.*;

public class TransactionJournalTest {
    private static final EntityKey KEY=new EntityKey("Asset",42L);

    @Test public void entityScopedPendingHistoryDoesNotConfuseUnrelatedWorkingOrJoinedMembers() {
        var first=new EntityMutationLedger(); var second=new EntityMutationLedger(); Object owner=new Object(); var other=new EntityKey("Asset",43L);
        first.set(KEY,"document","submitted"); var ticket=first.submit(owner); first.set(other,"title","unsubmitted"); first.set(KEY,"title","later");
        assertTrue(first.hasUncommittedChanges(KEY)); assertFalse(first.hasUncommittedChanges(other));
        second.moveEntityFrom(first,KEY,owner); assertTrue(second.hasUncommittedChanges(KEY)); assertFalse(second.hasUncommittedChanges(other));
        ticket.complete(COMMITTED); assertFalse(first.hasUncommittedChanges(KEY)); assertFalse(second.hasUncommittedChanges(KEY));
        assertEquals("unsubmitted",first.get(other,"title")); assertEquals("later",second.get(KEY,"title"));
    }

    @Test public void committedJournalRetiresSubmittedBatchesAndPreservesLaterUnsubmittedEdits() {
        var ledger=new EntityMutationLedger(); Object owner=new Object(); ledger.set(KEY,"document","first");
        var first=ledger.submit(owner); assertTrue(ledger.hasUncommittedChanges()); assertTrue(ledger.currentChangeSet().isEmpty());
        ledger.set(KEY,"document","second"); var second=ledger.submit(owner); ledger.set(KEY,"title","late");
        first.complete(COMMITTED); assertFalse(ledger.hasUncommittedChanges()); assertEquals("late",ledger.get(KEY,"title"));
        second.complete(COMMITTED); assertEquals(Set.of("title"),ledger.changedFieldNames(KEY));
        ledger.requireSubmissionOwner(new Object());
    }

    @Test public void reverseRollbackRestoresLatestIntentAndEarliestOptimisticBaseline() {
        var ledger=new EntityMutationLedger(); Object owner=new Object(); ledger.set(KEY,"document","first"); ledger.setOriginalVersion(KEY,7L);
        var first=ledger.submit(owner); ledger.set(KEY,"document","second"); ledger.setOriginalVersion(KEY,8L);
        var second=ledger.submit(owner); ledger.set(KEY,"title","late");
        assertThrows(TeaQLRuntimeException.class,() -> first.complete(ROLLED_BACK));
        second.complete(ROLLED_BACK); first.complete(ROLLED_BACK);
        assertEquals("second",ledger.get(KEY,"document")); assertEquals("late",ledger.get(KEY,"title"));
        assertEquals(Long.valueOf(7),ledger.getOriginalVersion(KEY)); assertFalse(ledger.hasUncommittedChanges());
    }

    @Test public void pendingJournalCannotBeDiscardedMovedOrUsedByAnotherPhysicalOwner() {
        var ledger=new EntityMutationLedger(); Object owner=new Object(); ledger.set(KEY,"document","value"); var submission=ledger.submit(owner);
        assertThrows(TeaQLRuntimeException.class,ledger::clearCurrentChangeSet);
        assertThrows(TeaQLRuntimeException.class,ledger::pushChangeSet); assertThrows(TeaQLRuntimeException.class,ledger::popChangeSet);
        assertThrows(TeaQLRuntimeException.class,() -> ledger.submit(new Object()));
        ledger.set(KEY,"title","later"); var different=new EntityMutationLedger();
        assertThrows(TeaQLRuntimeException.class,() -> different.mergeEntityFrom(ledger,KEY));
        submission.complete(ROLLED_BACK); assertEquals("later",ledger.get(KEY,"title"));
    }

    @Test public void unknownOutcomeRetainsJournalAndRefusesFurtherSubmissionWithoutGuessing() {
        var ledger=new EntityMutationLedger(); Object owner=new Object(); ledger.set(KEY,"document","value"); var submitted=ledger.submit(owner);
        submitted.complete(UNKNOWN); assertTrue(ledger.hasUnknownTransactionOutcome()); assertTrue(ledger.hasUncommittedChanges());
        assertEquals(UNKNOWN,assertThrows(TransactionCompletionException.class,() -> ledger.requireSubmissionOwner(owner)).outcome());
        assertEquals(UNKNOWN,assertThrows(TransactionCompletionException.class,() -> ledger.submit(new Object())).outcome());
        assertThrows(TeaQLRuntimeException.class,ledger::clearCurrentChangeSet); assertThrows(TeaQLRuntimeException.class,() -> submitted.complete(COMMITTED));
    }

    @Test public void lifecycleRebasesDeleteRecoveryAndCancelledCreationOntoOriginalState() {
        var ledger=new EntityMutationLedger(); ledger.markAsRecover(KEY); ledger.rebaseAfterRollback(KEY,1L,false); assertTrue(ledger.recoveredKeys().isEmpty());
        ledger.markAsDelete(KEY); ledger.rebaseAfterRollback(KEY,-1L,false); assertTrue(ledger.deletedKeys().isEmpty());
        ledger.markAsNew(KEY); ledger.set(KEY,"title","new"); ledger.markAsDelete(KEY);
        assertTrue(ledger.rebaseAfterRollback(KEY,null,true)); assertTrue(ledger.newKeys().isEmpty()); assertTrue(ledger.currentChangeSet().isEmpty());
    }

    @Test public void scopedMoveRoutesHistoricalRollbackToCurrentOwnerWithoutImportingOtherKeys() {
        var first=new EntityMutationLedger(); var second=new EntityMutationLedger(); Object owner=new Object(); var root=new EntityKey("Root",7L);
        first.set(KEY,"document","first"); first.setOriginalVersion(KEY,1L); var one=first.submit(owner);
        first.set(root,"title","not approved for second"); first.set(KEY,"document","second");
        assertTrue(second.moveEntityFrom(first,KEY,owner)); assertNull(second.get(root,"title")); assertEquals("not approved for second",first.get(root,"title"));
        second.setOriginalVersion(KEY,2L); var two=second.submit(owner); second.set(KEY,"title","late");
        two.complete(ROLLED_BACK); one.complete(ROLLED_BACK);
        assertEquals("second",second.get(KEY,"document")); assertEquals("late",second.get(KEY,"title")); assertEquals(Long.valueOf(1),second.getOriginalVersion(KEY));
        assertNull(first.get(KEY,"document")); assertEquals("not approved for second",first.get(root,"title"));
    }

    @Test public void joiningTwoPendingCohortsUsesPhysicalSubmissionOrderAndRetiresAllOnCommit() {
        var first=new EntityMutationLedger(); var second=new EntityMutationLedger(); Object owner=new Object(); var other=new EntityKey("Asset",43L);
        first.set(KEY,"document","one"); var one=first.submit(owner);
        second.set(other,"title","two"); var two=second.submit(owner);
        first.set(KEY,"document","three"); second.moveEntityFrom(first,KEY,owner); var three=second.submit(owner);
        first.set(new EntityKey("Root",1L),"title","keep");
        one.complete(COMMITTED); assertFalse(first.hasUncommittedChanges()); assertFalse(second.hasUncommittedChanges());
        two.complete(COMMITTED); three.complete(COMMITTED); assertEquals("keep",first.get(new EntityKey("Root",1L),"title"));
    }

    @Test public void joinedRollbackRestoresBothCohortsInActualGlobalSubmissionOrder() {
        var first=new EntityMutationLedger(); var second=new EntityMutationLedger(); Object owner=new Object(); var other=new EntityKey("Asset",43L);
        first.set(KEY,"document","one"); var one=first.submit(owner);
        second.set(other,"title","two"); var two=second.submit(owner);
        first.set(KEY,"document","three"); second.moveEntityFrom(first,KEY,owner); var three=second.submit(owner);
        three.complete(ROLLED_BACK); two.complete(ROLLED_BACK); one.complete(ROLLED_BACK);
        assertEquals("three",second.get(KEY,"document")); assertEquals("two",second.get(other,"title")); assertNull(first.get(KEY,"document"));
    }

    @Test public void attemptRegisteredBeforeWritingRetainsIntentAndPoisonsJoinedMembersOnUnknown() {
        var first=new EntityMutationLedger(); var second=new EntityMutationLedger(); Object owner=new Object(); first.set(KEY,"document","attempt");
        var attempt=first.beginSubmission(owner); assertEquals("attempt",first.get(KEY,"document"));
        first.set(KEY,"document","newer"); second.moveEntityFrom(first,KEY,owner); attempt.complete(UNKNOWN);
        assertTrue(first.hasUnknownTransactionOutcome()); assertTrue(second.hasUnknownTransactionOutcome());
        assertThrows(TransactionCompletionException.class,() -> second.requireSubmissionOwner(owner));
    }

    @Test public void foreignOwnerCannotMoveEvenAReadOnlyPendingMemberAndLeavesBothLedgersIntact() {
        var first=new EntityMutationLedger(); var second=new EntityMutationLedger(); Object owner=new Object(); first.set(KEY,"document","first");
        var submitted=first.submit(owner);
        assertThrows(TeaQLRuntimeException.class,() -> second.moveEntityFrom(first,KEY,new Object()));
        assertFalse(second.hasUncommittedChanges()); first.set(KEY,"title","late");
        assertThrows(TeaQLRuntimeException.class,() -> second.moveEntityFrom(first,KEY,null));
        submitted.complete(ROLLED_BACK); assertEquals("late",first.get(KEY,"title")); assertTrue(second.currentChangeSet().isEmpty());
    }

    @Test public void independentlySubmittedConflictingHistoriesRejectBeforeMovingWorkingIntent() {
        Object owner=new Object(); var first=new EntityMutationLedger(); var second=new EntityMutationLedger();
        first.set(KEY,"document","first history"); var one=first.submit(owner);
        second.set(KEY,"document","second history"); var two=second.submit(owner); first.set(KEY,"title","still first");
        assertThrows(TeaQLRuntimeException.class,() -> second.moveEntityFrom(first,KEY,owner));
        assertEquals("still first",first.get(KEY,"title")); assertNull(second.get(KEY,"title"));
        two.complete(ROLLED_BACK); one.complete(ROLLED_BACK); assertEquals("first history",first.get(KEY,"document")); assertEquals("second history",second.get(KEY,"document"));
    }
}
