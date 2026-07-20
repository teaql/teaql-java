package io.teaql.core.checker;

import java.util.ArrayList;
import java.util.List;

import io.teaql.core.utils.CollStreamUtil;
import io.teaql.core.utils.StrUtil;

public class CheckException extends RuntimeException {

    List<CheckResult> violates = new ArrayList<>();

    public CheckException() {
        super();
    }

    public CheckException(String message) {
        super(message);
    }

    public CheckException(String message, Throwable cause) {
        super(message, cause);
    }

    public CheckException(Throwable cause) {
        super(cause);
    }

    protected CheckException(
            String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public CheckException(List<CheckResult> pErrors) {
        this(
                StrUtil.join(
                        ";\n", CollStreamUtil.toList(pErrors, CheckResult::getNaturalLanguageStatement)));
        this.violates = pErrors;
    }

    public List<CheckResult> getViolates() {
        return violates;
    }

    public void setViolates(List<CheckResult> pViolates) {
        violates = pViolates;
    }
}
