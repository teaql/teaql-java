
package com.example.schoolmanagementservice;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.example.schoolmanagementservice.platform.PlatformRequest<com.example.schoolmanagementservice.platform.Platform> platforms(){
      return new com.example.schoolmanagementservice.platform.PlatformRequest(com.example.schoolmanagementservice.platform.Platform.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.schoolmanagementservice.platform.PlatformRequest<com.example.schoolmanagementservice.platform.Platform> platformsWithMinimalFields(){
      return new com.example.schoolmanagementservice.platform.PlatformRequest(com.example.schoolmanagementservice.platform.Platform.class).withVersion(Operator.GREATER_THAN, 0l);
  }


  public static com.example.schoolmanagementservice.schooltype.SchoolTypeRequest<com.example.schoolmanagementservice.schooltype.SchoolType> schoolTypes(){
      return new com.example.schoolmanagementservice.schooltype.SchoolTypeRequest(com.example.schoolmanagementservice.schooltype.SchoolType.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.schoolmanagementservice.schooltype.SchoolTypeRequest<com.example.schoolmanagementservice.schooltype.SchoolType> schoolTypesWithMinimalFields(){
      return new com.example.schoolmanagementservice.schooltype.SchoolTypeRequest(com.example.schoolmanagementservice.schooltype.SchoolType.class).withVersion(Operator.GREATER_THAN, 0l);
  }


  public static com.example.schoolmanagementservice.school.SchoolRequest<com.example.schoolmanagementservice.school.School> schools(){
      return new com.example.schoolmanagementservice.school.SchoolRequest(com.example.schoolmanagementservice.school.School.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.example.schoolmanagementservice.school.SchoolRequest<com.example.schoolmanagementservice.school.School> schoolsWithMinimalFields(){
      return new com.example.schoolmanagementservice.school.SchoolRequest(com.example.schoolmanagementservice.school.School.class).withVersion(Operator.GREATER_THAN, 0l);
  }


}