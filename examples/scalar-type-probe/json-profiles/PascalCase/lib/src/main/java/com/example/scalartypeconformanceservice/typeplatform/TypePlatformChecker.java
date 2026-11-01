
package com.example.scalartypeconformanceservice.typeplatform;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbeChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class TypePlatformChecker implements Checker<TypePlatform>{

    public String type(){
        return TypePlatform.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, TypePlatform typePlatform, ObjectLocation _parentLocation){
        if(needCheck(_context, typePlatform)){
            markAsChecked(_context, typePlatform);
            doCheck(_context, typePlatform, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, TypePlatform typePlatform, ObjectLocation _parentLocation){
      if((typePlatform == null)){
         return;
      }
      if(typePlatform.needPersist()){
      }
      if(typePlatform.newItem()){
      }else if(typePlatform.updateItem()){
        if(!typePlatform.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }

      }
      checkName(_context, typePlatform.getProperty(TypePlatform.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      for(int i = 0; typePlatform.getScalarTypeProbeList() != null && i < typePlatform.getScalarTypeProbeList().size(); i++){
         ScalarTypeProbe scalarTypeProbe = typePlatform.getScalarTypeProbeList().get(i);
         new ScalarTypeProbeChecker().checkAndFix(_context, scalarTypeProbe, newLocation(_parentLocation, "scalar_type_probe_list", i));
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