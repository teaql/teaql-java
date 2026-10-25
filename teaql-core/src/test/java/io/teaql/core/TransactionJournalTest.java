package io.teaql.core;

import java.util.*;
import org.junit.Test;
import static io.teaql.core.TransactionCompletion.Outcome.*;
import static org.junit.Assert.*;

public class TransactionJournalTest {
    private static final EntityKey KEY=new EntityKey("Asset",42L);

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
}
