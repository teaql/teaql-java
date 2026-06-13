package io.teaql.core.web;

import io.teaql.core.Entity;

public class WebStyle {
    public static final String STYLE = "style";
    public String backgroundColor;
    public String color;

    public String classNames;

    public static WebStyle withBackgroundColor(String color) {
        WebStyle style = new WebStyle();
        style.setBackgroundColor(color);
        return style;
    }

    public static WebStyle withClassNames(String classNames) {
        WebStyle style = new WebStyle();
        style.setClassNames(classNames);
        return style;
    }

    public static WebStyle withFontColor(String color) {
        WebStyle style = new WebStyle();
        style.setBackgroundColor(color);
        return style;
    }

    public String getClassNames() {
        return classNames;
    }

    public void setClassNames(String classNames) {
        this.classNames = classNames;
    }

    public void bind(Entity entity) {
        if (entity == null) {
            return;
        }
        entity.appendDynamicProperty(STYLE, this);
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
