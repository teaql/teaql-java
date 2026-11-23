
package com.example.contextfixservice.entry;

import com.example.contextfixservice.catalog.Catalog;
import com.example.contextfixservice.catalog.CatalogChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.time.LocalDateTime;

public class EntryChecker implements Checker<Entry>{

    public String type(){
        return Entry.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, Entry entry, ObjectLocation _parentLocation){
        if(needCheck(_context, entry)){
            markAsChecked(_context, entry);
            doCheck(_context, entry, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, Entry entry, ObjectLocation _parentLocation){
      if((entry == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, entry);
      if(entry.needPersist()){
      }
      if(entry.newItem()){
        if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "born_at"), () -> {
           entry.updateBornAt(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "born_at",
              "now".equalsIgnoreCase("now") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("now") ? "graphClock" : "contextFunction:now"));
           });
        }if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "touched_at"), () -> {
           entry.updateTouchedAt(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "touched_at",
              "now".equalsIgnoreCase("now") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("now") ? "graphClock" : "contextFunction:now"));
           });
        }if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "guarded_token"), () -> {
           entry.updateGuardedToken(_context.evaluate("uniStr", "prefix"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "guarded_token",
              "now".equalsIgnoreCase("uniStr") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("uniStr") ? "graphClock" : "contextFunction:uniStr"));
           });
        }
      }else if(entry.updateItem()){
        if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "touched_at"), () -> {
           entry.updateTouchedAt(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "touched_at",
              "now".equalsIgnoreCase("now") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("now") ? "graphClock" : "contextFunction:now"));
           });
        }
        if(!entry.isPropertyLoaded("catalog")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "catalog"), "Mutation requires a fully loaded entity");
        }
        if(!entry.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }
        if(!entry.isPropertyLoaded("born_at")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "born_at"), "Mutation requires a fully loaded entity");
        }
        if(!entry.isPropertyLoaded("touched_at")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "touched_at"), "Mutation requires a fully loaded entity");
        }
        if(!entry.isPropertyLoaded("source_ip")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "source_ip"), "Mutation requires a fully loaded entity");
        }
        if(!entry.isPropertyLoaded("last_ip")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "last_ip"), "Mutation requires a fully loaded entity");
        }
        if(!entry.isPropertyLoaded("opened_by")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "opened_by"), "Mutation requires a fully loaded entity");
        }
        if(!entry.isPropertyLoaded("changed_by")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "changed_by"), "Mutation requires a fully loaded entity");
        }
        if(!entry.isPropertyLoaded("guarded_token")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "guarded_token"), "Mutation requires a fully loaded entity");
        }

      }
      checkCatalog(_context, entry.getProperty(Entry.CATALOG_PROPERTY), newLocation(_parentLocation, "catalog"));
      checkName(_context, entry.getProperty(Entry.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      checkBornAt(_context, entry.getProperty(Entry.BORN_AT_PROPERTY), newLocation(_parentLocation, "born_at"));
      checkTouchedAt(_context, entry.getProperty(Entry.TOUCHED_AT_PROPERTY), newLocation(_parentLocation, "touched_at"));
      checkSourceIp(_context, entry.getProperty(Entry.SOURCE_IP_PROPERTY), newLocation(_parentLocation, "source_ip"));
      checkLastIp(_context, entry.getProperty(Entry.LAST_IP_PROPERTY), newLocation(_parentLocation, "last_ip"));
      checkOpenedBy(_context, entry.getProperty(Entry.OPENED_BY_PROPERTY), newLocation(_parentLocation, "opened_by"));
      checkChangedBy(_context, entry.getProperty(Entry.CHANGED_BY_PROPERTY), newLocation(_parentLocation, "changed_by"));
      checkGuardedToken(_context, entry.getProperty(Entry.GUARDED_TOKEN_PROPERTY), newLocation(_parentLocation, "guarded_token"));
    }

    public void checkCatalog(UserContext _context, Catalog catalog, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, catalog);
    if((catalog == null)){
        return;
    }
    new CatalogChecker().checkAndFix(_context, catalog, _parentLocation);
    }
    public void checkName(UserContext _context, String name, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, name);

    }
    public void checkBornAt(UserContext _context, LocalDateTime bornAt, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, bornAt);
    if((bornAt == null)){
        return;
    }
    }
    public void checkTouchedAt(UserContext _context, LocalDateTime touchedAt, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, touchedAt);
    if((touchedAt == null)){
        return;
    }
    }
    public void checkSourceIp(UserContext _context, String sourceIp, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, sourceIp);
    if((sourceIp == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, sourceIp);

    }
    public void checkLastIp(UserContext _context, String lastIp, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, lastIp);
    if((lastIp == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, lastIp);

    }
    public void checkOpenedBy(UserContext _context, String openedBy, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, openedBy);
    if((openedBy == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, openedBy);

    }
    public void checkChangedBy(UserContext _context, String changedBy, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, changedBy);
    if((changedBy == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, changedBy);

    }
    public void checkGuardedToken(UserContext _context, String guardedToken, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, guardedToken);
    if((guardedToken == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, guardedToken);

    }
}