
package com.teaql.runtimeexampleconformanceservice.workitem;

import com.teaql.runtimeexampleconformanceservice.platform.Platform;
import com.teaql.runtimeexampleconformanceservice.platform.PlatformChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class WorkItemChecker implements Checker<WorkItem>{

    public String type(){
        return WorkItem.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, WorkItem workItem, ObjectLocation _parentLocation){
        if(needCheck(_context, workItem)){
            markAsChecked(_context, workItem);
            doCheck(_context, workItem, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, WorkItem workItem, ObjectLocation _parentLocation){
      if((workItem == null)){
         return;
      }
      if(workItem.newItem()){
      }else if(workItem.updateItem()){
      }
      checkTitle(_context, workItem.getProperty(WorkItem.TITLE_PROPERTY), newLocation(_parentLocation, WorkItem.TITLE_PROPERTY));
      checkDescription(_context, workItem.getProperty(WorkItem.DESCRIPTION_PROPERTY), newLocation(_parentLocation, WorkItem.DESCRIPTION_PROPERTY));
      checkPlatform(_context, workItem.getProperty(WorkItem.PLATFORM_PROPERTY), newLocation(_parentLocation, WorkItem.PLATFORM_PROPERTY));
    }

    public void checkTitle(UserContext _context, String title, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, title);
    if((title == null)){
        return;
    }
    minStringCheck(_context, _parentLocation, 1, title);
    maxStringCheck(_context, _parentLocation, 80, title);

    }
    public void checkDescription(UserContext _context, String description, ObjectLocation _parentLocation){
    if((description == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, description);

    }
    public void checkPlatform(UserContext _context, Platform platform, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, platform);
    if((platform == null)){
        return;
    }
    new PlatformChecker().checkAndFix(_context, platform, _parentLocation);
    }
}