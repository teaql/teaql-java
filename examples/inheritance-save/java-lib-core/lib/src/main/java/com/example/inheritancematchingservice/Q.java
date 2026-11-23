
package com.example.inheritancematchingservice;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.example.inheritancematchingservice.platform.PlatformRequest<com.example.inheritancematchingservice.platform.Platform> platforms(){
      return new com.example.inheritancematchingservice.platform.PlatformRequest(com.example.inheritancematchingservice.platform.Platform.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.inheritancematchingservice.platform.PlatformRequest<com.example.inheritancematchingservice.platform.Platform> platformsWithMinimalFields(){
      return new com.example.inheritancematchingservice.platform.PlatformRequest(com.example.inheritancematchingservice.platform.Platform.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.example.inheritancematchingservice.schooltype.SchoolTypeRequest<com.example.inheritancematchingservice.schooltype.SchoolType> schoolTypes(){
      return new com.example.inheritancematchingservice.schooltype.SchoolTypeRequest(com.example.inheritancematchingservice.schooltype.SchoolType.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.inheritancematchingservice.schooltype.SchoolTypeRequest<com.example.inheritancematchingservice.schooltype.SchoolType> schoolTypesWithMinimalFields(){
      return new com.example.inheritancematchingservice.schooltype.SchoolTypeRequest(com.example.inheritancematchingservice.schooltype.SchoolType.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.example.inheritancematchingservice.school.SchoolRequest<com.example.inheritancematchingservice.school.School> schools(){
      return new com.example.inheritancematchingservice.school.SchoolRequest(com.example.inheritancematchingservice.school.School.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.inheritancematchingservice.school.SchoolRequest<com.example.inheritancematchingservice.school.School> schoolsWithMinimalFields(){
      return new com.example.inheritancematchingservice.school.SchoolRequest(com.example.inheritancematchingservice.school.School.class).withVersion(Operator.GREATER_THAN, 0l);
  }


  public static com.example.inheritancematchingservice.school.SchoolRequest<com.example.inheritancematchingservice.school.School> schoolsWithChildren(){
      com.example.inheritancematchingservice.school.SchoolRequest<com.example.inheritancematchingservice.school.School> school = new com.example.inheritancematchingservice.school.SchoolRequest(com.example.inheritancematchingservice.school.School.class).unselectVersion().unlimited().selectSelfFields();
      school.enhanceChild(
      academiesWithChildren()
      );
      school.enhanceChild(
      new com.example.inheritancematchingservice.college.CollegeRequest(com.example.inheritancematchingservice.college.College.class).unselectVersion().unlimited().selectSelfFields()
      );
      return school;
  }

  public static com.example.inheritancematchingservice.academy.AcademyRequest<com.example.inheritancematchingservice.academy.Academy> academies(){
      return new com.example.inheritancematchingservice.academy.AcademyRequest(com.example.inheritancematchingservice.academy.Academy.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.inheritancematchingservice.academy.AcademyRequest<com.example.inheritancematchingservice.academy.Academy> academiesWithMinimalFields(){
      return new com.example.inheritancematchingservice.academy.AcademyRequest(com.example.inheritancematchingservice.academy.Academy.class).withVersion(Operator.GREATER_THAN, 0l);
  }


  public static com.example.inheritancematchingservice.academy.AcademyRequest<com.example.inheritancematchingservice.academy.Academy> academiesWithChildren(){
      com.example.inheritancematchingservice.academy.AcademyRequest<com.example.inheritancematchingservice.academy.Academy> academy = new com.example.inheritancematchingservice.academy.AcademyRequest(com.example.inheritancematchingservice.academy.Academy.class).unselectVersion().unlimited().selectSelfFields();
      academy.enhanceChild(
      new com.example.inheritancematchingservice.trainingacademy.TrainingAcademyRequest(com.example.inheritancematchingservice.trainingacademy.TrainingAcademy.class).unselectVersion().unlimited().selectSelfFields()
      );
      return academy;
  }

  public static com.example.inheritancematchingservice.trainingacademy.TrainingAcademyRequest<com.example.inheritancematchingservice.trainingacademy.TrainingAcademy> trainingAcademies(){
      return new com.example.inheritancematchingservice.trainingacademy.TrainingAcademyRequest(com.example.inheritancematchingservice.trainingacademy.TrainingAcademy.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.inheritancematchingservice.trainingacademy.TrainingAcademyRequest<com.example.inheritancematchingservice.trainingacademy.TrainingAcademy> trainingAcademiesWithMinimalFields(){
      return new com.example.inheritancematchingservice.trainingacademy.TrainingAcademyRequest(com.example.inheritancematchingservice.trainingacademy.TrainingAcademy.class).withVersion(Operator.GREATER_THAN, 0l);
  }



  public static com.example.inheritancematchingservice.college.CollegeRequest<com.example.inheritancematchingservice.college.College> colleges(){
      return new com.example.inheritancematchingservice.college.CollegeRequest(com.example.inheritancematchingservice.college.College.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.inheritancematchingservice.college.CollegeRequest<com.example.inheritancematchingservice.college.College> collegesWithMinimalFields(){
      return new com.example.inheritancematchingservice.college.CollegeRequest(com.example.inheritancematchingservice.college.College.class).withVersion(Operator.GREATER_THAN, 0l);
  }



}