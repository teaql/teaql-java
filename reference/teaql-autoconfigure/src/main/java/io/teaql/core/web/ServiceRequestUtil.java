package io.teaql.core.web;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import io.teaql.core.utils.CollectionUtil;
import io.teaql.core.utils.BooleanUtil;
import io.teaql.core.utils.ClassUtil;
import io.teaql.core.utils.ObjectUtil;
import io.teaql.core.utils.ReflectUtil;
import io.teaql.core.utils.StrUtil;

import static io.teaql.core.web.UITemplateRender.serviceRequestPopupKey;

import io.teaql.core.BaseEntity;
import io.teaql.core.BaseRequest;
import io.teaql.core.Entity;
import io.teaql.core.EntityStatus;
import io.teaql.core.SmartList;
import io.teaql.core.TQLException;
import io.teaql.core.UserContext;
import io.teaql.core.DefaultUserContext;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.Relation;

public class ServiceRequestUtil {

    public static final String SERVICE_REQUEST = "serviceRequestDescriptor";

    public static <T extends BaseEntity> List<T> getDBViews(UserContext ctx, T view) {
        if (view == null) {
            return Collections.emptyList();
        }
        reloadRequest(ctx, view);
        BaseEntity serviceRequest = getServiceRequest(ctx, view);
        Relation serviceRequestRelation = getServiceRequestRelation(ctx, view);
        SmartList<T> dbViews =
                serviceRequest.getProperty(serviceRequestRelation.getReverseProperty().getName());
        if (dbViews != null) {
            for (T dbView : dbViews) {
                dbView.setProperty(serviceRequestRelation.getName(), serviceRequest);
            }
        }
        else {
            return Collections.emptyList();
        }
        return dbViews.getData();
    }

    private static <T extends BaseEntity> Relation getServiceRequestRelation(
            UserContext ctx, T view) {
        EntityDescriptor entityDescriptor = ctx.resolveEntityDescriptor(view.typeName());
        List<Relation> properties = entityDescriptor.getOwnRelations();
        for (Relation relation : properties) {
            String isServiceRequest =
                    relation
                            .getReverseProperty()
                            .getOwner()
                            .getAdditionalInfo()
                            .getOrDefault(SERVICE_REQUEST, "false");
            if (BooleanUtil.toBoolean(isServiceRequest)) {
                return relation;
            }
        }
        return null;
    }

    public static <T> T getFirst(UserContext ctx, BaseEntity view, String property) {
        List<BaseEntity> dbViews = getDBViews(ctx, view);
        for (BaseEntity dbView : dbViews) {
            Object o = dbView.getProperty(property);
            if (o != null) {
                return (T) o;
            }
        }
        return null;
    }

    public static <T> T getLast(UserContext ctx, BaseEntity view, String property) {
        List<T> list = getList(ctx, view, property);
        if (CollectionUtil.isEmpty(list)) {
            return null;
        }
        return CollectionUtil.getLast(list);
    }

    public static <T extends BaseEntity> T getLast(UserContext ctx, T view) {
        List<T> list = getDBViews(ctx, view);
        if (CollectionUtil.isEmpty(list)) {
            return null;
        }
        return CollectionUtil.getLast(list);
    }

    // switch to another view
    public static <T extends BaseEntity> T gotoView(
            UserContext ctx, BaseEntity view, Class<T> targetViewType) {

        // reload the service request from the view
        reloadRequest(ctx, view);
        BaseEntity serviceRequest = getServiceRequest(ctx, view);
        EntityDescriptor serviceRequestDescriptor =
                ctx.resolveEntityDescriptor(serviceRequest.typeName());

        // find the target view relationship
        List<Relation> foreignRelations = serviceRequestDescriptor.getForeignRelations();
        Relation relationship = null;
        for (Relation foreignRelation : foreignRelations) {
            Class aClass = foreignRelation.getRelationKeeper().getTargetType();
            if (targetViewType.isAssignableFrom(aClass)) {
                relationship = foreignRelation;
                break;
            }
        }

        // get the views of the target view type
        SmartList<T> values = serviceRequest.getProperty(relationship.getName());
        if (values != null) {
            T targetView = values.first();
            // set the service request of the target view
            targetView.setProperty(relationship.getReverseProperty().getName(), serviceRequest);
            return targetView;
        }
        return null;
    }

    public static <T extends BaseEntity> T getFirst(UserContext ctx, T view) {
        List<BaseEntity> dbViews = getDBViews(ctx, view);
        if (dbViews.isEmpty()) {
            return null;
        }
        return (T) dbViews.get(0);
    }

    public static <T extends BaseEntity> T getFirst(UserContext ctx, T view, Predicate<T> filter) {
        List<T> dbViews = getDBViews(ctx, view);
        if (dbViews.isEmpty()) {
            return null;
        }
        for (T dbView : dbViews) {
            if (filter.test(dbView)) {
                return dbView;
            }
        }
        return null;
    }

    public static <T> List<T> getList(UserContext ctx, BaseEntity view, String property) {
        List<BaseEntity> dbViews = getDBViews(ctx, view);

        List<T> ret = new ArrayList<>();
        for (BaseEntity dbView : dbViews) {
            Object o = dbView.getProperty(property);
            if (o != null) {
                ret.add((T) o);
            }
        }
        return ret;
    }

    public static <T extends BaseEntity> T reloadRequest(UserContext ctx, T view) {
        if (view == null) {
            return null;
        }
        Relation serviceRequestRelation = getServiceRequestRelation(ctx, view);
        if (serviceRequestRelation == null) {
            throw new TQLException("no service request defined on the view:" + view.typeName());
        }

        BaseEntity request = view.getProperty(serviceRequestRelation.getName());
        if (request == null) {
            return view;
        }

        if (request.get$status().equals(EntityStatus.REFER)) {
            Class<? extends Entity> targetType =
                    serviceRequestRelation.getReverseProperty().getOwner().getTargetType();
            BaseRequest loadRequest =
                    ReflectUtil.newInstance(
                             ClassUtil.loadClass(StrUtil.format("{}Request", targetType.getName())), targetType);
            loadRequest.selectSelf();
            loadRequest.appendSearchCriteria(
                    loadRequest.createBasicSearchCriteria(
                            BaseEntity.ID_PROPERTY, Operator.EQUAL, request.getId()));
            request = (BaseEntity) loadRequest.executeForOne(ctx);
            view.setProperty(serviceRequestRelation.getName(), request);
        }
        return view;
    }

    public static BaseEntity getServiceRequest(UserContext ctx, BaseEntity view) {
        if (view == null) {
            return null;
        }
        Relation serviceRequestRelation = getServiceRequestRelation(ctx, view);
        if (serviceRequestRelation == null) {
            throw new TQLException("no service request defined on the view:" + view.typeName());
        }
        return view.getProperty(serviceRequestRelation.getName());
    }

    // save request on the view
    public static <T extends BaseEntity> T saveRequest(
            UserContext ctx, T view, ViewOption viewOption) {
        if (view == null || viewOption == null) {
            return null;
        }
        BaseEntity request = getServiceRequest(ctx, view);
        if (request == null) {
            return view;
        }

        if (viewOption.needReload()) {
            reloadRequest(ctx, view);
            request = getServiceRequest(ctx, view);
        }

        Relation serviceRequestRelation = getServiceRequestRelation(ctx, view);
        String name = serviceRequestRelation.getName();
        if (viewOption.isSaveView()) {
            if (viewOption.isOverride()) {
                request.setProperty(serviceRequestRelation.getReverseProperty().getName(), null);
            }
            request.addRelation(serviceRequestRelation.getReverseProperty().getName(), view);
        }

        if (viewOption.isSave()) {
            saveRequestInView(ctx, request);
        }
        view.setProperty(name, request);

        if (!viewOption.isFirst()) {
            return view;
        }

        SmartList l = request.getProperty(serviceRequestRelation.getReverseProperty().getName());
        Entity first = l.first();
        if (first != null) {
            first.setProperty(name, request);
        }
        return (T) first;
    }

    private static void saveRequestInView(UserContext ctx, BaseEntity request) {
        String property = findJsonMeProperty(ctx, request);
        // update the jsonMe property to null, and trigger save for the request
        Method method =
                ReflectUtil.getMethodByName(
                        request.getClass(), StrUtil.upperFirstAndAddPre(property, "update"));
        ReflectUtil.invoke(request, method, (Object) null);

        // clean up the request reference in views
        EntityDescriptor serviceRequestDescriptor = ctx.resolveEntityDescriptor(request.typeName());
        List<Relation> foreignRelations = serviceRequestDescriptor.getForeignRelations();
        for (Relation foreignRelation : foreignRelations) {
            Object subData = request.getProperty(foreignRelation.getName());
            if (subData instanceof SmartList l) {
                for (Object o : l) {
                    BaseEntity item = (BaseEntity) o;
                    Relation serviceRequestRelation = getServiceRequestRelation(ctx, item);
                    String name = serviceRequestRelation.getName();
                    item.setProperty(name, null);
                }
            }
            else if (subData instanceof BaseEntity e) {
                Relation serviceRequestRelation = getServiceRequestRelation(ctx, e);
                String name = serviceRequestRelation.getName();
                e.setProperty(name, null);
            }
        }
        request.save(ctx);
    }

    private static <T extends BaseEntity> String findJsonMeProperty(UserContext ctx, T request) {
        EntityDescriptor serviceRequestDescriptor = ctx.resolveEntityDescriptor(request.typeName());
        List<PropertyDescriptor> properties = serviceRequestDescriptor.getProperties();
        for (PropertyDescriptor propertyDescriptor : properties) {
            if (propertyDescriptor.getClass().getSimpleName().equalsIgnoreCase("JsonMeProperty")) {
                return propertyDescriptor.getName();
            }
        }
        return null;
    }

    public static <T extends BaseEntity> T removeView(
            UserContext ctx, T view, String property, Object value) {
        return removeView(ctx, view, v -> ObjectUtil.equals(value, v.getProperty(property)));
    }

    public static <T extends BaseEntity> T clear(UserContext ctx, T view) {
        return removeView(ctx, view, x -> true);
    }

    public static <T extends BaseEntity> T removeView(UserContext ctx, T view, Predicate<T> filter) {
        if (view == null) {
            return null;
        }

        reloadRequest(ctx, view);
        Relation serviceRequestRelation = getServiceRequestRelation(ctx, view);
        BaseEntity request = getServiceRequest(ctx, view);
        SmartList list = request.getProperty(serviceRequestRelation.getReverseProperty().getName());
        if (list == null) {
            return view;
        }
        boolean updated = list.removeIf(filter);
        if (updated) {
            saveRequestInView(ctx, request);
        }
        view.setProperty(serviceRequestRelation.getName(), request);
        return view;
    }

    public static Object showPop(UserContext ctx, BaseEntity view) {
        if (view == null) {
            return null;
        }
        BaseEntity requestObj = getServiceRequest(ctx, view);
        if (requestObj == null) {
            return null;
        }
        if (ctx instanceof DefaultUserContext dctx) {
            return dctx.getAndRemoveInStore(serviceRequestPopupKey(requestObj));
        }
        return null;
    }

    public enum ViewOption {

        // Bit1：1 save request，0 read only
        // Bit2：1 save current view, 0 ignore current view
        // Bit3：1 override view, 0 append current view
        // Bit4: 1 return first view，0 return current view
        OVERRIDE_FIRST(0b1111),
        OVERRIDE_CURRENT(0b1110),
        APPEND_FIRST(0b1101),
        APPEND_CURRENT(0b1100),
        NO_SAVE_FIRST(0b0001),
        NO_SAVE_CURRENT(0b0000),
        SAVE_FIRST(0b1001),
        SAVE_CURRENT(0b1000);

        int code;

        ViewOption(int code) {
            this.code = code;
        }

        public boolean isOverride() {
            return (this.code & 0b10) != 0;
        }

        public boolean isSaveView() {
            return (this.code & 0b100) != 0;
        }

        public boolean isSave() {
            return (this.code & 0b1000) != 0;
        }

        public boolean isFirst() {
            return (this.code & 0b1) != 0;
        }

        public boolean needReload() {
            return isSave() || isFirst();
        }
    }
}
