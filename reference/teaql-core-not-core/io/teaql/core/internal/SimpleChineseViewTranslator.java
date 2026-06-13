package io.teaql.core.internal;

import io.teaql.core.Entity;
import io.teaql.core.NaturalLanguageTranslator;
import io.teaql.core.TQLException;
import io.teaql.core.checker.CheckResult;
import io.teaql.core.checker.HashLocation;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.utils.StrUtil;
import java.util.List;

public class SimpleChineseViewTranslator implements NaturalLanguageTranslator {

    EntityMetaFactory metaFactory;

    public SimpleChineseViewTranslator(EntityMetaFactory pMetaFactory) {
        metaFactory = pMetaFactory;
    }

    @Override
    public List<CheckResult> translateError(Entity entity, List<CheckResult> errors) {
        for (CheckResult error : errors) {
            translate(entity, error);
        }
        return errors;
    }

    private void translate(Entity entity, CheckResult error) {
        switch (error.getRuleId()) {
            case MIN:
                translateMin(entity, error);
                break;
            case MAX:
                translateMax(entity, error);
                break;
            case MIN_STR_LEN:
                translateMinStrLen(entity, error);
                break;
            case MAX_STR_LEN:
                translateMaxStrLen(entity, error);
                break;
            case MIN_DATE:
                translateMinDate(entity, error);
                break;
            case MAX_DATE:
                translateMaxDate(entity, error);
                break;
            case REQUIRED:
                translateRequired(entity, error);
                break;
        }
    }

    private void translateMin(Entity entity, CheckResult error) {
        String message =
                StrUtil.format(
                        "{}需要不能小于{}，输入为{}",
                        translateLocation(entity, error),
                        error.getSystemValue(),
                        error.getInputValue());
        error.setNaturalLanguageStatement(message);
    }

    private Object translateLocation(Entity entity, CheckResult error) {
        EntityDescriptor entityDescriptor = metaFactory.resolveEntityDescriptor(entity.typeName());
        if (error.getLocation() instanceof HashLocation hashLocation) {
            String member = hashLocation.getMember();
            PropertyDescriptor property = entityDescriptor.findProperty(member);
            return property.getStr("zh_CN", member);
        }
        throw new TQLException("未知错误");
    }

    private void translateMax(Entity entity, CheckResult error) {
        String message =
                StrUtil.format(
                        "{}需要不能大于{}，输入为{}",
                        translateLocation(entity, error),
                        error.getSystemValue(),
                        error.getInputValue());
        error.setNaturalLanguageStatement(message);
    }

    private void translateMinStrLen(Entity entity, CheckResult error) {
        String message =
                StrUtil.format(
                        "{}长度不能小于{},输入的{}的长度是{}",
                        translateLocation(entity, error),
                        error.getSystemValue(),
                        error.getInputValue(),
                        StrUtil.length((CharSequence) error.getInputValue()));
        error.setNaturalLanguageStatement(message);
    }

    private void translateMaxStrLen(Entity entity, CheckResult error) {
        String message =
                StrUtil.format(
                        "{}长度不能大于{},输入的{}的长度是{}",
                        translateLocation(entity, error),
                        error.getSystemValue(),
                        error.getInputValue(),
                        StrUtil.length((CharSequence) error.getInputValue()));
        error.setNaturalLanguageStatement(message);
    }

    private void translateMinDate(Entity entity, CheckResult error) {
        String message =
                StrUtil.format(
                        "{}不能早于{},输入的是{}",
                        translateLocation(entity, error),
                        error.getSystemValue(),
                        error.getInputValue());
        error.setNaturalLanguageStatement(message);
    }

    private void translateMaxDate(Entity entity, CheckResult error) {
        String message =
                StrUtil.format(
                        "{}不能晚于{},输入的是{}",
                        translateLocation(entity, error),
                        error.getSystemValue(),
                        error.getInputValue());
        error.setNaturalLanguageStatement(message);
    }

    private void translateRequired(Entity entity, CheckResult error) {
        String message = StrUtil.format("{}是必填项", translateLocation(entity, error));
        error.setNaturalLanguageStatement(message);
    }
}
