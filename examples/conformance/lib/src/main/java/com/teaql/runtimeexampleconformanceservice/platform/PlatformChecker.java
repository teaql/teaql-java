
package com.teaql.runtimeexampleconformanceservice.platform;

import com.teaql.runtimeexampleconformanceservice.workitem.WorkItem;
import com.teaql.runtimeexampleconformanceservice.workitem.WorkItemChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class PlatformChecker implements Checker<Platform>{

    public String type(){
        return Platform.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, Platform platform, ObjectLocation _parentLocation){
        if(needCheck(_context, platform)){
            markAsChecked(_context, platform);
            doCheck(_context, platform, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, Platform platform, ObjectLocation _parentLocation){
      if((platform == null)){
         return;
      }
      if(platform.newItem()){
      }else if(platform.updateItem()){
      }
      checkName(_context, platform.getProperty(Platform.NAME_PROPERTY), newLocation(_parentLocation, Platform.NAME_PROPERTY));
      for(int i = 0; platform.getWorkItemList() != null && i < platform.getWorkItemList().size(); i++){
         WorkItem workItem = platform.getWorkItemList().get(i);
         new WorkItemChecker().checkAndFix(_context, workItem, newLocation(_parentLocation, Platform.WORK_ITEM_LIST_PROPERTY, i));
      }
    }

    public void checkName(UserContext _context, String name, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, name);

    }
}