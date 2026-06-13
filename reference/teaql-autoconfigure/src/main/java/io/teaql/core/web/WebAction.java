package io.teaql.core.web;

import java.util.ArrayList;
import java.util.List;

import io.teaql.core.Entity;

public class WebAction {

    public static final String ACTION_LIST = "actionList";
    private String key;
    private String name;
    private String level;
    private String execute;
    private String target;
    private String component;
    private String warningMessage;
    private String roleForList;
    private String requestURL;

    public WebAction() {
    }

    public static WebAction viewWebAction() {
        WebAction webAction = new WebAction();
        webAction.setName("VIEW DETAIL");
        webAction.setLevel("view");
        webAction.setExecute("switchview");
        webAction.setTarget("detail");
        return webAction;
    }

    public static WebAction viewSubListAction(String name, String listViewName, String roleForList) {
        WebAction webAction = new WebAction();
        webAction.setName(name);
        webAction.setLevel("view");
        webAction.setExecute("gotoList");
        webAction.setRoleForList(roleForList);
        webAction.setTarget(listViewName);
        return webAction;
    }

    public static WebAction simpleComponentAction(String name, String componentName) {
        WebAction webAction = new WebAction();
        webAction.setName(name);
        webAction.setComponent(componentName);
        return webAction;
    }

    public static WebAction modifyWebAction(String name, String url, String warningMessage) {
        WebAction webAction = new WebAction();
        webAction.setName(name);
        webAction.setKey(name);
        webAction.setLevel("modify");
        webAction.setExecute("switchview");
        webAction.setTarget("modify");
        webAction.setWarningMessage(warningMessage);
        webAction.setRequestURL(url);
        return webAction;
    }

    public static WebAction modifyWebAction(String name, String url) {
        return modifyWebAction(name, url, null);
    }

    public static WebAction modifyWebAction(String url) {
        return modifyWebAction("web.action.update", url);
    }

    public static WebAction deleteWebAction(String url, String warningMessage) {

        return modifyWebAction("web.action.delete", url, warningMessage);
    }

    public static WebAction auditWebAction(String url, String warningMessage) {

        return modifyWebAction("AUDIT", url, warningMessage);
    }

    public static WebAction discardWebAction(String url, String warningMessage) {

        return modifyWebAction("DISCARD", url, warningMessage);
    }

    public static WebAction gotoAction(String name, String target, String url) {
        WebAction webAction = new WebAction();
        webAction.setName(name);
        webAction.setLevel("modify");
        webAction.setExecute("gotoview");
        webAction.setTarget(target);
        webAction.setRequestURL(url);
        return webAction;
    }

    public static WebAction switchViewAction(String viewName, String target) {
        WebAction webAction = new WebAction();
        webAction.setName(viewName);
        webAction.setLevel("modify");
        webAction.setExecute("switchview");
        webAction.setTarget(target);
        return webAction;
    }

    public static WebAction modifyWebAction() {
        WebAction webAction = new WebAction();
        webAction.setName("UPDATE");
        webAction.setLevel("modify");
        webAction.setExecute("switchview");
        webAction.setTarget("modify");
        return webAction;
    }

    public static WebAction addNewWebAction(String odName) {
        WebAction webAction = new WebAction();
        webAction.setName("NEW " + odName);
        webAction.setLevel("modify");
        webAction.setExecute("switchview");
        webAction.setTarget("addnew");
        return webAction;
    }

    public static WebAction deleteWebAction() {
        WebAction webAction = new WebAction();
        webAction.setName("DELETE");
        webAction.setLevel("delete");
        webAction.setExecute("switchview");
        webAction.setTarget("deleteview");
        return webAction;
    }

    public static List<WebAction> commonWebActions() {

        List<WebAction> webActions = new ArrayList<>();

        webActions.add(viewWebAction());
        webActions.add(modifyWebAction());

        return webActions;
    }

    public static WebAction batchUploadWebAction() {
        WebAction webAction = new WebAction();
        webAction.setName("BATCH UPLOAD");
        webAction.setLevel("modify");
        webAction.setExecute("switchview");
        webAction.setTarget("batchupload");
        return webAction;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }

    public String getRoleForList() {
        return roleForList;
    }

    public void setRoleForList(String roleForList) {
        this.roleForList = roleForList;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getRequestURL() {
        return requestURL;
    }

    public void setRequestURL(String requestURL) {
        this.requestURL = requestURL;
    }

    public void bind(Entity entity) {
        if (entity != null) {
            entity.appendDynamicProperty(ACTION_LIST, this);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getExecute() {
        return execute;
    }

    public void setExecute(String execute) {
        this.execute = execute;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String pKey) {
        key = pKey;
    }
}
