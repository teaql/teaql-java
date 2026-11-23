
package com.example.checkernaturalmessageservice.entry;

import com.example.checkernaturalmessageservice.catalog.Catalog;
import com.example.checkernaturalmessageservice.catalog.CatalogChecker;
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
      if(entry.needPersist()){
      }
      if(entry.newItem()){
        if(entry.getCreateTime() == null){
           entry.updateCreateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "create_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        }if(entry.getUpdateTime() == null){
           entry.updateUpdateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        }
      }else if(entry.updateItem()){
        entry.updateUpdateTime(_context.evaluate("now"));
        _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        if(!entry.isPropertyLoaded("catalog")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "catalog"), "Mutation requires a fully loaded entity");
        }
        if(!entry.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }
        if(!entry.isPropertyLoaded("quantity")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "quantity"), "Mutation requires a fully loaded entity");
        }
        if(!entry.isPropertyLoaded("description")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "description"), "Mutation requires a fully loaded entity");
        }
        if(!entry.isPropertyLoaded("protected_note")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "protected_note"), "Mutation requires a fully loaded entity");
        }
        if(!entry.isPropertyLoaded("create_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "create_time"), "Mutation requires a fully loaded entity");
        }
        if(!entry.isPropertyLoaded("update_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "update_time"), "Mutation requires a fully loaded entity");
        }

      }
      checkCatalog(_context, entry.getProperty(Entry.CATALOG_PROPERTY), newLocation(_parentLocation, "catalog"));
      checkName(_context, entry.getProperty(Entry.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      checkQuantity(_context, entry.getProperty(Entry.QUANTITY_PROPERTY), newLocation(_parentLocation, "quantity"));
      checkDescription(_context, entry.getProperty(Entry.DESCRIPTION_PROPERTY), newLocation(_parentLocation, "description"));
      checkProtectedNote(_context, entry.getProperty(Entry.PROTECTED_NOTE_PROPERTY), newLocation(_parentLocation, "protected_note"));
      checkCreateTime(_context, entry.getProperty(Entry.CREATE_TIME_PROPERTY), newLocation(_parentLocation, "create_time"));
      checkUpdateTime(_context, entry.getProperty(Entry.UPDATE_TIME_PROPERTY), newLocation(_parentLocation, "update_time"));
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
    public void checkQuantity(UserContext _context, Integer quantity, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, quantity);
    if((quantity == null)){
        return;
    }
    }
    public void checkDescription(UserContext _context, String description, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, description);
    if((description == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, description);

    }
    public void checkProtectedNote(UserContext _context, String protectedNote, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, protectedNote);
    if((protectedNote == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, protectedNote);

    }
    public void checkCreateTime(UserContext _context, LocalDateTime createTime, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, createTime);
    if((createTime == null)){
        return;
    }
    }
    public void checkUpdateTime(UserContext _context, LocalDateTime updateTime, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, updateTime);
    if((updateTime == null)){
        return;
    }
    }
}