
package com.example.inheritancematchingservice.trainingacademy;

import com.example.inheritancematchingservice.academy.AcademyChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class TrainingAcademyChecker implements Checker<TrainingAcademy>{

    public String type(){
        return TrainingAcademy.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, TrainingAcademy trainingAcademy, ObjectLocation _parentLocation){
        if(needCheck(_context, trainingAcademy)){
            markAsChecked(_context, trainingAcademy);
            new AcademyChecker().doCheck(_context, trainingAcademy, _parentLocation);

            doCheck(_context, trainingAcademy, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, TrainingAcademy trainingAcademy, ObjectLocation _parentLocation){
      if((trainingAcademy == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, trainingAcademy);
      if(trainingAcademy.needPersist()){
      }
      if(trainingAcademy.newItem()){
      }else if(trainingAcademy.updateItem()){
        if(!trainingAcademy.isPropertyLoaded("specialty")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "specialty"), "Mutation requires a fully loaded entity");
        }

      }
      checkSpecialty(_context, trainingAcademy.getProperty(TrainingAcademy.SPECIALTY_PROPERTY), newLocation(_parentLocation, "specialty"));
    }

    public void checkSpecialty(UserContext _context, String specialty, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, specialty);
    if((specialty == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, specialty);

    }
}