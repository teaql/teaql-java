package io.teaql.blob.reference;

import io.teaql.core.*;
import io.teaql.core.blob.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Owns the outer SQL command transaction. Replay never invokes the business action. */
public final class PostgresBlobCommandExecutor implements BlobCommandService {
    private final PostgresBlobCommandJournal journal;
    private final BlobCommandAuthorization authorization;
    private final Clock clock;
    public PostgresBlobCommandExecutor(PostgresBlobCommandJournal journal,BlobCommandAuthorization authorization,Clock clock) {
        this.journal=Objects.requireNonNull(journal); this.authorization=Objects.requireNonNull(authorization); this.clock=Objects.requireNonNull(clock);
    }
    @Override public Execution execute(UserContext context,Request request,Action action) {
        Objects.requireNonNull(request); Objects.requireNonNull(action); BlobCommandScope.requireAbsent(); journal.requireIdle();
        var identity=authorization.identity(context); authorization.requireExecute(context,request); authorization.requireSame(context,identity);
        Execution result=journal.transaction(() -> {
            Object owner=journal.owner();
            try (var scope=journal.openScope()) {
                var claim=journal.claim(identity,request); Receipt receipt;
                if (claim.fresh()) {
                    var revisions=action.run(); journal.requireOwner(owner); authorization.requireSame(context,identity); authorization.requireExecute(context,request);
                    receipt=new Receipt(identity.scope(),request.operation(),request.invocationKey(),request.fingerprint(),clock.instant().truncatedTo(ChronoUnit.MILLIS),revisions);
                    authorization.requireReplay(context,receipt); authorization.requireSame(context,identity); journal.complete(identity,request,receipt,owner);
                } else {
                    receipt=claim.receipt(); authorization.requireReplay(context,receipt); authorization.requireSame(context,identity);
                }
                return new Execution(receipt,!claim.fresh());
            }
        });
        try { authorization.requireSame(context,identity); authorization.requireReplay(context,result.receipt()); authorization.requireSame(context,identity); }
        catch (RuntimeException denied) { throw new TransactionCompletionException(TransactionCompletion.Outcome.COMMITTED,denied); }
        return result;
    }
    @Override public Optional<Receipt> resolve(UserContext context,Request request) {
        Objects.requireNonNull(request); BlobCommandScope.requireAbsent(); journal.requireIdle();
        var identity=authorization.identity(context); var receipt=journal.find(identity,request);
        receipt.ifPresent(value -> authorization.requireReplay(context,value)); authorization.requireSame(context,identity); return receipt;
    }
}
