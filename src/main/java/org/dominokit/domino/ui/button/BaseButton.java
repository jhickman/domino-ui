package org.dominokit.domino.ui.button;

import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLElement;
import elemental2.dom.Node;
import elemental2.dom.Text;
import org.dominokit.domino.ui.icons.Icon;
import org.dominokit.domino.ui.style.Color;
import org.dominokit.domino.ui.style.StyleType;
import org.dominokit.domino.ui.style.WaveStyle;
import org.dominokit.domino.ui.style.WavesElement;
import org.dominokit.domino.ui.utils.*;
import org.jboss.gwt.elemento.core.Elements;
import org.jboss.gwt.elemento.core.IsElement;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.jboss.gwt.elemento.core.Elements.span;

public abstract class BaseButton<B extends BaseButton<?>> extends WavesElement<HTMLElement, B> implements
        HasClickableElement, Sizable<B>, HasBackground<B>,
        HasContent<B>, Switchable<B> {

    private static final String DISABLED = "disabled";

    protected final HTMLElement buttonElement = Elements.button().css("btn").asElement();
    private StyleType type;
    private Color background;
    private Color color;
    private ButtonSize size;
    protected String content;
    private Icon icon;
    private HTMLElement textSpan = span().asElement();
    private Text textElement = DomGlobal.document.createTextNode("");

    protected BaseButton() {
    }

    protected BaseButton(String content) {
        this();
        setContent(content);
    }

    protected BaseButton(Icon icon) {
        this();
        setIcon(icon);
    }

    protected BaseButton(String content, StyleType type) {
        this(content);
        setButtonType(type);
    }

    protected BaseButton(Icon icon, StyleType type) {
        this(icon);
        setButtonType(type);
    }

    public BaseButton(String content, Color background) {
        this(content);
        setBackground(background);
    }

    @Override
    public B setContent(String content) {
        this.content = content;
        textElement.textContent = content;
        if (isNull(icon)) {
            buttonElement.appendChild(textElement);
        }else{
            textSpan.appendChild(textElement);
            buttonElement.appendChild(textSpan);
        }
        return (B) this;
    }

    @Override
    public B setTextContent(String text) {
        setContent(text);
        return (B) this;
    }

    public B setSize(ButtonSize size) {
        if (nonNull(this.size))
            buttonElement.classList.remove("btn-" + this.size.getStyle());
        buttonElement.classList.add("btn-" + size.getStyle());
        this.size = size;
        return (B) this;
    }

    public B setBlock(boolean block) {
        if (block)
            buttonElement.classList.add("btn-block");
        else
            buttonElement.classList.remove("btn-block");
        return (B) this;
    }

    @Override
    public B setBackground(Color background) {
        if (nonNull(this.type))
            buttonElement.classList.remove("btn-" + this.type.getStyle());
        if (nonNull(this.background))
            buttonElement.classList.remove(this.background.getBackground());
        buttonElement.classList.add(background.getBackground());
        this.background = background;
        return (B) this;
    }

    public B setColor(Color color) {
        if (nonNull(this.color))
            asElement().classList.remove(this.color.getStyle());
        this.color = color;
        asElement().classList.add(this.color.getStyle());
        return (B) this;
    }

    public B setButtonType(StyleType type) {
        if (nonNull(this.type))
            buttonElement.classList.remove("btn-" + this.type.getStyle());
        buttonElement.classList.add("btn-" + type.getStyle());
        this.type = type;
        return (B) this;
    }

    @Override
    public B disable() {
        buttonElement.setAttribute(DISABLED, DISABLED);
        return (B) this;
    }

    @Override
    public B enable() {
        buttonElement.removeAttribute(DISABLED);
        return (B) this;
    }

    @Override
    public boolean isEnabled() {
        return !buttonElement.hasAttribute(DISABLED);
    }

    @Override
    public HTMLElement getClickableElement() {
        return asElement();
    }

    /**
     * @deprecated use {@link #appendChild(Node)}
     */
    @Deprecated
    public B appendContent(Node node) {
        this.asElement().appendChild(node);
        return (B) this;
    }

    public B appendChild(Node node) {
        this.asElement().appendChild(node);
        return (B) this;
    }

    public B appendChild(IsElement element) {
        this.asElement().appendChild(element.asElement());
        return (B) this;
    }

    @Override
    public B large() {
        setSize(ButtonSize.LARGE);
        return (B) this;
    }

    @Override
    public B small() {
        setSize(ButtonSize.SMALL);
        return (B) this;
    }

    @Override
    public B xSmall() {
        setSize(ButtonSize.XSMALL);
        return (B) this;
    }

    public B block() {
        setBlock(true);
        return (B) this;
    }

    public B linkify() {
        buttonElement.classList.add("btn-link");
        return (B) this;
    }

    public B deLinkify() {
        buttonElement.classList.remove("btn-link");
        return (B) this;
    }

    public B circle(CircleSize size) {
        buttonElement.classList.add(size.getStyle());
        applyCircleWaves();
        return (B) this;
    }

    public B setIcon(Icon icon) {
        if (nonNull(this.icon)) {
            this.icon.asElement().textContent = icon.getName();
        } else {
            if (nonNull(content) && !content.isEmpty()) {
                textSpan.appendChild(textElement);
                buttonElement.appendChild(textSpan.appendChild(textElement));
            }
            this.icon = icon;
            buttonElement.appendChild(this.icon.asElement());
        }
        return (B) this;
    }

    private void applyCircleWaves() {
        applyWaveStyle(WaveStyle.CIRCLE);
        applyWaveStyle(WaveStyle.FLOAT);
    }
}
