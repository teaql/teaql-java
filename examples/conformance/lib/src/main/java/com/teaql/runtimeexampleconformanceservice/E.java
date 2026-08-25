
package com.teaql.runtimeexampleconformanceservice;

import com.teaql.runtimeexampleconformanceservice.platform.Platform;
import com.teaql.runtimeexampleconformanceservice.platform.PlatformExpression;
import com.teaql.runtimeexampleconformanceservice.workitem.WorkItem;
import com.teaql.runtimeexampleconformanceservice.workitem.WorkItemExpression;
import io.teaql.core.value.ValueExpression;

public class E  {
  public static PlatformExpression<Platform, Platform, Platform> platform(Platform platform){
      return new PlatformExpression(new ValueExpression(platform));
  }
  public static WorkItemExpression<WorkItem, WorkItem, WorkItem> workItem(WorkItem workItem){
      return new WorkItemExpression(new ValueExpression(workItem));
  }
}