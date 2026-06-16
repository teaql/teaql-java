package io.teaql.tools.impl;

import io.teaql.core.UserContext;
import io.teaql.core.tools.AgentHttpTool;
import io.teaql.core.tools.ExecutableHttpTool;
import io.teaql.core.tools.HttpIntentPhase;

// Assuming HttpUtil exists in teaql-utils or we'll mock the print out for now
// import io.teaql.core.utils.io.HttpUtil;

public class AgentHttpToolImpl implements AgentHttpTool {
    
    private final UserContext ctx;
    
    public AgentHttpToolImpl(UserContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public HttpIntentPhase get(String url) {
        return new HttpIntentPhaseImpl("GET", url, null);
    }

    @Override
    public HttpIntentPhase post(String url, Object body) {
        return new HttpIntentPhaseImpl("POST", url, body);
    }
    
    private class HttpIntentPhaseImpl implements HttpIntentPhase {
        private final String method;
        private final String url;
        private final Object body;
        
        HttpIntentPhaseImpl(String method, String url, Object body) {
            this.method = method;
            this.url = url;
            this.body = body;
        }

        @Override
        public ExecutableHttpTool purpose(String purposeMessage) {
            return new ExecutableHttpToolImpl(method, url, body, "PURPOSE: " + purposeMessage);
        }

        @Override
        public ExecutableHttpTool auditAs(String auditMessage) {
            return new ExecutableHttpToolImpl(method, url, body, "AUDIT: " + auditMessage);
        }
    }
    
    private class ExecutableHttpToolImpl implements ExecutableHttpTool {
        private final String method;
        private final String url;
        private final Object body;
        private final String intent;
        
        ExecutableHttpToolImpl(String method, String url, Object body, String intent) {
            this.method = method;
            this.url = url;
            this.body = body;
            this.intent = intent;
        }

        @Override
        public String execute() {
            // 1. Audit Log 拦截
            String identity = (ctx != null) ? String.valueOf(ctx.hashCode()) : "UNKNOWN_AGENT";
            System.out.printf("[AUDIT LOG] User/Agent [%s] is executing HTTP %s to [%s] with intent [%s]%n",
                    identity, method, url, intent);
            
            // 2. 委托底层 HttpUtil 真正执行
            // return HttpUtil.request(method, url, body);
            return "SUCCESS_MOCK_RESPONSE_FOR_NOW";
        }
    }
}
