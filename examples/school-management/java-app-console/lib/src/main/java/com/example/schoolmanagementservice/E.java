
package com.example.schoolmanagementservice;

import com.example.schoolmanagementservice.platform.Platform;
import com.example.schoolmanagementservice.platform.PlatformExpression;
import com.example.schoolmanagementservice.school.School;
import com.example.schoolmanagementservice.school.SchoolExpression;
import com.example.schoolmanagementservice.schooltype.SchoolType;
import com.example.schoolmanagementservice.schooltype.SchoolTypeExpression;
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
}