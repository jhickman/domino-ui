package org.dominokit.domino.ui.layout;

import elemental2.dom.HTMLElement;
import org.dominokit.domino.ui.utils.BaseDominoElement;
import org.jboss.gwt.elemento.template.DataElement;
import org.jboss.gwt.elemento.template.Templated;

import javax.annotation.PostConstruct;

@Templated
public abstract class Section extends BaseDominoElement<HTMLElement, Section> {
    @DataElement
    HTMLElement leftSide;

    @DataElement
    HTMLElement rightSide;

    @PostConstruct
    void init(){
        init(this);
    }

    public static Section create(){
        return new Templated_Section();
    }
}
