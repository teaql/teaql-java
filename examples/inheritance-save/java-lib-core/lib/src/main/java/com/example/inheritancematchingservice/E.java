
package com.example.inheritancematchingservice;

import com.example.inheritancematchingservice.academy.Academy;
import com.example.inheritancematchingservice.academy.AcademyExpression;
import com.example.inheritancematchingservice.college.College;
import com.example.inheritancematchingservice.college.CollegeExpression;
import com.example.inheritancematchingservice.platform.Platform;
import com.example.inheritancematchingservice.platform.PlatformExpression;
import com.example.inheritancematchingservice.school.School;
import com.example.inheritancematchingservice.school.SchoolExpression;
import com.example.inheritancematchingservice.schooltype.SchoolType;
import com.example.inheritancematchingservice.schooltype.SchoolTypeExpression;
import com.example.inheritancematchingservice.trainingacademy.TrainingAcademy;
import com.example.inheritancematchingservice.trainingacademy.TrainingAcademyExpression;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.ValueExpression;

public class E  {
  public static PlatformExpression<Platform, Platform, Platform> platform(Platform platform){
      return new PlatformExpression(new ValueExpression(platform, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.platform(<root>)"));
  }
  public static SchoolTypeExpression<SchoolType, SchoolType, SchoolType> schoolType(SchoolType schoolType){
      return new SchoolTypeExpression(new ValueExpression(schoolType, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.schoolType(<root>)"));
  }
  public static SchoolExpression<School, School, School> school(School school){
      return new SchoolExpression(new ValueExpression(school, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.school(<root>)"));
  }
  public static AcademyExpression<Academy, Academy, Academy> academy(Academy academy){
      return new AcademyExpression(new ValueExpression(academy, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.academy(<root>)"));
  }
  public static TrainingAcademyExpression<TrainingAcademy, TrainingAcademy, TrainingAcademy> trainingAcademy(TrainingAcademy trainingAcademy){
      return new TrainingAcademyExpression(new ValueExpression(trainingAcademy, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.trainingAcademy(<root>)"));
  }
  public static CollegeExpression<College, College, College> college(College college){
      return new CollegeExpression(new ValueExpression(college, JsonFieldNamingProfile.fromModelValue("camelCase"), "E.college(<root>)"));
  }
}