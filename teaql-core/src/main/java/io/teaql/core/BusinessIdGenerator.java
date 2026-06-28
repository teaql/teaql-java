package io.teaql.core;

import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;

/**
 * 通用业务 ID 生成器接口。
 * 用于生成像“订单号”、“物流单号”这样带有规则和业务语义的字符串 ID。
 */
public interface BusinessIdGenerator {

    /**
     * 生成业务字符串 ID。
     *
     * @param ctx          当前用户上下文
     * @param entity       当前正在操作的实体实例
     * @param entityDesc   实体元数据描述
     * @param propertyDesc 需要生成 ID 的字段元数据描述
     * @return 格式化后的业务序列号
     */
    String generateBusinessId(UserContext ctx, Entity entity, EntityDescriptor entityDesc, PropertyDescriptor propertyDesc);
}
