package org.dominokit.domino.ui.utils;

import elemental2.dom.HTMLElement;
import org.jboss.gwt.elemento.core.IsElement;

public class DominoElement<E extends HTMLElement> extends BaseDominoElement<E, DominoElement<E>> {

    private final E wrappedElement;

    public static <E extends HTMLElement> DominoElement<E> of(E node){
        return new DominoElement<>(node);
    }

    public static <E extends HTMLElement> DominoElement<E> of(IsElement<E> node){
        return new DominoElement<>(node.asElement());
    }

    public DominoElement(E element) {
        this.wrappedElement = element;
        init(this);
    }

    @Override
    public E asElement() {
        return wrappedElement;
    }
}
