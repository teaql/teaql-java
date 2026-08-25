
package com.teaql.runtimeexampleconformanceservice;

import io.teaql.core.criteria.Operator;

public class Q  {
  public static com.teaql.runtimeexampleconformanceservice.platform.PlatformRequest<com.teaql.runtimeexampleconformanceservice.platform.Platform> platforms(){
      return new com.teaql.runtimeexampleconformanceservice.platform.PlatformRequest(com.teaql.runtimeexampleconformanceservice.platform.Platform.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.runtimeexampleconformanceservice.platform.PlatformRequest<com.teaql.runtimeexampleconformanceservice.platform.Platform> platformsWithMinimalFields(){
      return new com.teaql.runtimeexampleconformanceservice.platform.PlatformRequest(com.teaql.runtimeexampleconformanceservice.platform.Platform.class).withVersion(Operator.GREATER_THAN, 0l);
  }


  public static com.teaql.runtimeexampleconformanceservice.workitem.WorkItemRequest<com.teaql.runtimeexampleconformanceservice.workitem.WorkItem> workItems(){
      return new com.teaql.runtimeexampleconformanceservice.workitem.WorkItemRequest(com.teaql.runtimeexampleconformanceservice.workitem.WorkItem.class).selectSelf().withVersion(Operator.GREATER_THAN, 0l);
  }
  public static com.teaql.runtimeexampleconformanceservice.workitem.WorkItemRequest<com.teaql.runtimeexampleconformanceservice.workitem.WorkItem> workItemsWithMinimalFields(){
      return new com.teaql.runtimeexampleconformanceservice.workitem.WorkItemRequest(com.teaql.runtimeexampleconformanceservice.workitem.WorkItem.class).withVersion(Operator.GREATER_THAN, 0l);
  }


}