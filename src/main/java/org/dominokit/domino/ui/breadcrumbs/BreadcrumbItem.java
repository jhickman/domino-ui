package org.dominokit.domino.ui.breadcrumbs;

import elemental2.dom.*;
import org.dominokit.domino.ui.icons.Icon;
import org.dominokit.domino.ui.utils.BaseDominoElement;
import org.dominokit.domino.ui.utils.HasClickableElement;

import static java.util.Objects.nonNull;
import static org.jboss.gwt.elemento.core.Elements.a;
import static org.jboss.gwt.elemento.core.Elements.li;

public class BreadcrumbItem extends BaseDominoElement<HTMLLIElement, BreadcrumbItem> implements HasClickableElement {

    private HTMLLIElement element = li().asElement();
    private HTMLAnchorElement anchorElement = a().asElement();
    private Text textElement;
    private Icon icon;
    private boolean active = false;

    private BreadcrumbItem(String text) {
        init(text, null);
    }

    private BreadcrumbItem(String text, Icon icon) {
        init(text, icon);
    }

    private void init(String text, Icon icon) {
        init(this);
        this.textElement = DomGlobal.document.createTextNode(text);
        if (nonNull(icon)) {
            this.icon = icon;
            this.anchorElement.appendChild(icon.asElement());
        }
        this.anchorElement.appendChild(textElement);
        element.appendChild(anchorElement);
        init(this);
    }

    public static BreadcrumbItem create(String text) {
        return new BreadcrumbItem(text);
    }

    public static BreadcrumbItem create(Icon icon, String text) {
        return new BreadcrumbItem(text, icon);
    }

    public BreadcrumbItem activate() {
        if (!active) {
            element.classList.add("active");
            textElement.remove();
            anchorElement.remove();
            if (nonNull(icon)) {
                icon.asElement().remove();
                element.appendChild(icon.asElement());
            }
            element.appendChild(textElement);
            this.active = true;
        }

        return this;
    }

    public BreadcrumbItem deActivate() {
        if (active) {
            element.classList.remove("active");
            textElement.remove();
            if (nonNull(icon)) {
                icon.asElement().remove();
                anchorElement.appendChild(icon.asElement());
            }
            anchorElement.appendChild(textElement);
            element.appendChild(anchorElement);
            this.active = false;
        }

        return this;
    }

    public BreadcrumbItem setActive(boolean active){
        if(active){
            return activate();
        }else{
            return deActivate();
        }
    }

    @Override
    public HTMLLIElement asElement() {
        return element;
    }

    @Override
    public HTMLAnchorElement getClickableElement() {
        return anchorElement;
    }

    public Text getTextElement() {
        return textElement;
    }

    public Icon getIcon() {
        return icon;
    }

    public boolean isActive() {
        return active;
    }
}