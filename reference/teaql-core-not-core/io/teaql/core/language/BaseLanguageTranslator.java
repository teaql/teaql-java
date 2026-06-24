package io.teaql.core.language;

import java.util.List;

import io.teaql.core.utils.NamingCase;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.Entity;
import io.teaql.core.NaturalLanguageTranslator;
import io.teaql.core.checker.ArrayLocation;
import io.teaql.core.checker.CheckResult;
import io.teaql.core.checker.HashLocation;
import io.teaql.core.checker.ObjectLocation;

public class BaseLanguageTranslator implements NaturalLanguageTranslator {

    private static io.teaql.core.utils.JSONObject i18nDict;
    private static boolean loaded = false;

    private static synchronized void loadDict() {
        if (loaded) {
            return;
        }
        try {
            String path = System.getProperty("teaql.i18n.path");
            String jsonStr;
            if (io.teaql.core.utils.StrUtil.isNotEmpty(path) && new java.io.File(path).exists()) {
                jsonStr = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)), java.nio.charset.StandardCharsets.UTF_8);
            } else {
                jsonStr = io.teaql.core.utils.ResourceUtil.readUtf8Str("teaql-i18n.json");
            }
            i18nDict = io.teaql.utils.json.JSONUtil.parseObj(jsonStr);
        } catch (Exception e) {
            i18nDict = new io.teaql.core.utils.JSONObject();
        }
        loaded = true;
    }

    private String languageKey;

    public BaseLanguageTranslator() {
        this(null);
    }

    public BaseLanguageTranslator(String languageKey) {
        if (languageKey != null) {
            this.languageKey = languageKey;
        } else {
            this.languageKey = resolveLanguageKeyFromClass();
        }

        String langKey = getLanguageKey();
        if (!"en".equals(langKey)) {
            String path = System.getProperty("teaql.i18n.path");
            if (io.teaql.core.utils.StrUtil.isEmpty(path)) {
                throw new IllegalStateException("Translation dictionary is required for non-English locale '" + langKey 
                    + "'. Please configure the JVM parameter -Dteaql.i18n.path pointing to the translated JSON file.");
            }
            if (!new java.io.File(path).exists()) {
                throw new IllegalStateException("The configured translation dictionary file at '" + path 
                    + "' does not exist. Please check the JVM parameter -Dteaql.i18n.path.");
            }
            loadDict();
            if (i18nDict == null || i18nDict.isEmpty()) {
                throw new IllegalStateException("The translation dictionary file at '" + path 
                    + "' could not be loaded or is empty.");
            }
        } else {
            loadDict();
        }
    }

    protected String getLanguageKey() {
        return this.languageKey != null ? this.languageKey : "en";
    }

    private String resolveLanguageKeyFromClass() {
        String className = this.getClass().getSimpleName();
        if (className.endsWith("Translator")) {
            String name = className.substring(0, className.length() - "Translator".length());
            switch (name) {
                case "Arabic": return "ar";
                case "Chinese": return "zh_CN";
                case "TraditionalChinese": return "zh_TW";
                case "Spanish": return "es";
                case "French": return "fr";
                case "German": return "de";
                case "Japanese": return "ja";
                case "Korean": return "ko";
                case "Portuguese": return "pt";
                case "Thai": return "th";
                case "Ukrainian": return "uk";
                case "Filipino": return "fil";
                case "Indonesian": return "id";
                case "English": return "en";
            }
        }
        return "en";
    }

    protected String lookupTranslation(String term, String languageKey) {
        loadDict();
        if (i18nDict == null || term == null || languageKey == null) {
            return null;
        }
        io.teaql.core.utils.JSONObject termObj = i18nDict.getJSONObject(term);
        if (termObj != null) {
            return termObj.getStr(languageKey);
        }
        return null;
    }

    @Override
    public List<CheckResult> translateError(Entity pEntity, List<CheckResult> errors) {
        for (CheckResult error : errors) {
            translate(error);
        }
        return errors;
    }
    protected void translate(CheckResult error) {
        switch (error.getRuleId()) {
            case MIN:
                translateMin(error);
                break;
            case MAX:
                translateMax(error);
                break;
            case MIN_STR_LEN:
                translateMinStrLen(error);
                break;
            case MAX_STR_LEN:
                translateMaxStrLen(error);
                break;
            case MIN_DATE:
                translateMinDate(error);
                break;
            case MAX_DATE:
                translateMaxDate(error);
                break;
            case REQUIRED:
                translateRequired(error);
                break;
        }
    }

    protected void translateMin(CheckResult error) {
        String template;
        String lang = getLanguageKey();
        switch (lang) {
            case "zh_CN":
            case "zh_TW":
                template = "{} 应该大于等于 {}，但输入值为 {}";
                break;
            case "es":
                template = "El/La {} debe ser igual o mayor que {}, pero el valor ingresado es {}";
                break;
            case "ar":
                template = "يجب أن يكون {} مساويًا أو أكبر من {}، لكن المُدخل هو {}";
                break;
            default:
                template = "The {} should be equal or greater than {}, but input is {}";
                break;
        }
        String message = StrUtil.format(template, translateLocation(error), error.getSystemValue(), error.getInputValue());
        error.setNaturalLanguageStatement(message);
    }

    protected Object translateLocation(CheckResult error) {
        return translateLocation(error.getLocation());
    }

    protected void translateMax(CheckResult error) {
        String template;
        String lang = getLanguageKey();
        switch (lang) {
            case "zh_CN":
            case "zh_TW":
                template = "{} 应该小于等于 {}，但输入值为 {}";
                break;
            case "es":
                template = "El/La {} debe ser igual o menor que {}, pero el valor ingresado es {}";
                break;
            case "ar":
                template = "يجب أن يكون {} مساويًا أو أصغر من {}، لكن المُدخل هو {}";
                break;
            default:
                template = "The {} should be equal or less than {}, but input is {} ";
                break;
        }
        String message = StrUtil.format(template, translateLocation(error), error.getSystemValue(), error.getInputValue());
        error.setNaturalLanguageStatement(message);
    }

    protected void translateMinStrLen(CheckResult error) {
        String template;
        String lang = getLanguageKey();
        switch (lang) {
            case "zh_CN":
            case "zh_TW":
                template = "{} 的长度应大于等于 {}，但实际长度为 {}";
                break;
            case "es":
                template = "La longitud de {} debe ser igual o mayor que {}, pero la longitud de {} es {}";
                break;
            case "ar":
                template = "يجب أن يكون طول {} مساويًا أو أكبر من {}، لكن طول {} هو {}";
                break;
            default:
                template = "The length of {} should be equal or greater than {}, but the length of {} is {}";
                break;
        }
        String message = StrUtil.format(template, translateLocation(error), error.getSystemValue(), error.getInputValue(), StrUtil.length((CharSequence) error.getInputValue()));
        error.setNaturalLanguageStatement(message);
    }

    protected void translateMaxStrLen(CheckResult error) {
        String template;
        String lang = getLanguageKey();
        switch (lang) {
            case "zh_CN":
            case "zh_TW":
                template = "{} 的长度应小于等于 {}，但实际长度为 {}";
                break;
            case "es":
                template = "La longitud de {} debe ser igual o menor que {}, pero la longitud de {} es {}";
                break;
            case "ar":
                template = "يجب أن يكون طول {} مساويًا أو أصغر من {}، لكن طول {} هو {}";
                break;
            default:
                template = "The length of {} should be equal or less than {}, but the length of {} is {}";
                break;
        }
        String message = StrUtil.format(template, translateLocation(error), error.getSystemValue(), error.getInputValue(), StrUtil.length((CharSequence) error.getInputValue()));
        error.setNaturalLanguageStatement(message);
    }

    protected void translateMinDate(CheckResult error) {
        String template;
        String lang = getLanguageKey();
        switch (lang) {
            case "zh_CN":
            case "zh_TW":
                template = "{} 应该在 {} 或之后，但输入值为 {}";
                break;
            case "es":
                template = "El/La {} debe ser en o después de {}, pero el valor ingresado es {}";
                break;
            case "ar":
                template = "يجب أن يكون {} في أو بعد {}، لكن المُدخل هو {}";
                break;
            default:
                template = "The {} should be at or after {}, but input is {}";
                break;
        }
        String message = StrUtil.format(template, translateLocation(error), error.getSystemValue(), error.getInputValue());
        error.setNaturalLanguageStatement(message);
    }

    protected void translateMaxDate(CheckResult error) {
        String template;
        String lang = getLanguageKey();
        switch (lang) {
            case "zh_CN":
            case "zh_TW":
                template = "{} 应该在 {} 或之前，但输入值为 {}";
                break;
            case "es":
                template = "El/La {} debe ser en o antes de {}, pero el valor ingresado es {}";
                break;
            case "ar":
                template = "يجب أن يكون {} في أو قبل {}، لكن المُدخل هو {}";
                break;
            default:
                template = "The {} should be at or before {}, but input is {}";
                break;
        }
        String message = StrUtil.format(template, translateLocation(error), error.getSystemValue(), error.getInputValue());
        error.setNaturalLanguageStatement(message);
    }

    protected void translateRequired(CheckResult error) {
        String template;
        String lang = getLanguageKey();
        switch (lang) {
            case "zh_CN":
            case "zh_TW":
                template = "{} 是必填项";
                break;
            case "es":
                template = "{} es requerido/a";
                break;
            case "ar":
                template = "{} مطلوب";
                break;
            default:
                template = "The {} is required";
                break;
        }
        String message = StrUtil.format(template, translateLocation(error));
        error.setNaturalLanguageStatement(message);
    }

    protected String translateLocation(ObjectLocation location) {
        if (location.isFirstLevel()) {
            return getSimpleLocation(location);
        }

        if (location.isSecondLevel()) {
            ObjectLocation parent = location.getParent();
            if (parent instanceof HashLocation) {
                String lang = getLanguageKey();
                switch (lang) {
                    case "zh_CN":
                    case "zh_TW":
                        return getSimpleLocation(location) + " 的 " + getSimpleLocation(parent);
                    case "es":
                        return StrUtil.format("{} de el/la {}", getSimpleLocation(location), getSimpleLocation(parent));
                    case "ar":
                        return StrUtil.format("{} لـ {}", getSimpleLocation(location), getSimpleLocation(parent));
                    default:
                        return StrUtil.format("{} of the {}", getSimpleLocation(location), getSimpleLocation(parent));
                }
            }
        }

        if (location.isThirdLevel()) {
            ObjectLocation parent = location.getParent();
            if (parent instanceof HashLocation) {
                String lang = getLanguageKey();
                switch (lang) {
                    case "zh_CN":
                    case "zh_TW":
                        return getSimpleLocation(location) + " 属性在 " + translateLocation(parent);
                    case "es":
                        return StrUtil.format("atributo {} dentro de el/la {}", getSimpleLocation(location), translateLocation(parent));
                    case "ar":
                        return StrUtil.format("سمة {} داخل {}", getSimpleLocation(location), translateLocation(parent));
                    default:
                        return StrUtil.format("{} attribute within the {}", getSimpleLocation(location), translateLocation(parent));
                }
            }

            if (parent instanceof ArrayLocation) {
                String lang = getLanguageKey();
                switch (lang) {
                    case "zh_CN":
                    case "zh_TW":
                        return getSimpleLocation(location) + " 属性在 " + getArrayLocation(parent);
                    case "es":
                        return StrUtil.format("atributo {} dentro de {}", getSimpleLocation(location), getArrayLocation(parent));
                    case "ar":
                        return StrUtil.format("سمة {} داخل {}", getSimpleLocation(location), getArrayLocation(parent));
                    default:
                        return StrUtil.format("{} attribute within the {}", getSimpleLocation(location), getArrayLocation(parent));
                }
            }
        }

        return location.toString();
    }

    protected Object getArrayLocation(ObjectLocation location) {
        if (location instanceof ArrayLocation) {
            String lang = getLanguageKey();
            switch (lang) {
                case "zh_CN":
                case "zh_TW":
                    return StrUtil.format("{} 的{}元素", ordinal(((ArrayLocation) location).getIndex()), translateLocation(location.getParent()));
                case "es":
                    return StrUtil.format("el/la {} elemento de {}", ordinal(((ArrayLocation) location).getIndex()), translateLocation(location.getParent()));
                case "ar":
                    return StrUtil.format("العنصر الـ {} من {}", ordinal(((ArrayLocation) location).getIndex()), translateLocation(location.getParent()));
                default:
                    return StrUtil.format("{} element of the {}", ordinal(((ArrayLocation) location).getIndex()), translateLocation(location.getParent()));
            }
        }
        return location.toString();
    }

    protected String getSimpleLocation(ObjectLocation location) {
        if (location instanceof HashLocation) {
            String member = ((HashLocation) location).getMember();
            String translation = lookupTranslation(member, getLanguageKey());
            if (translation != null) {
                return translation;
            }
            return convertToTitleCase(member);
        }
        return location.toString();
    }

    public static String convertToTitleCase(String input) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            if (i == 0 || Character.isUpperCase(currentChar)) {
                if (i != 0) {
                    result.append(" ");
                }
                result.append(Character.toUpperCase(currentChar));
            } else {
                result.append(Character.toLowerCase(currentChar));
            }
        }

        return result.toString();
    }

    public String ordinal(int index) {
        String lang = getLanguageKey();
        switch (lang) {
            case "zh_CN":
            case "zh_TW":
                return "第" + (index + 1) + "个";
            case "es":
                return (index + 1) + "º";
            case "ar":
                return (index + 1) + ".";
            default:
                int sequence = index + 1;
                String[] suffixes = new String[] {"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
                switch (sequence % 100) {
                    case 11:
                    case 12:
                    case 13:
                        return sequence + "th";
                    default:
                        return sequence + suffixes[sequence % 10];
                }
        }
    }
}
