package org.dominokit.domino.ui.forms;

import elemental2.dom.*;
import elemental2.dom.EventListener;
import elemental2.svg.SVGElement;
import jsinterop.base.Js;
import org.dominokit.domino.ui.style.Color;
import org.dominokit.domino.ui.style.Style;
import org.dominokit.domino.ui.utils.BaseDominoElement;
import org.dominokit.domino.ui.utils.DominoElement;
import org.dominokit.domino.ui.utils.Focusable;
import org.dominokit.domino.ui.utils.IsReadOnly;
import org.jboss.gwt.elemento.core.Elements;
import org.jboss.gwt.elemento.core.IsElement;
import org.jboss.gwt.elemento.template.DataElement;
import org.jboss.gwt.elemento.template.Templated;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static elemental2.dom.DomGlobal.document;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.dominokit.domino.ui.utils.ElementUtil.*;
import static org.jboss.gwt.elemento.core.Elements.div;
import static org.jboss.gwt.elemento.core.Elements.li;

public class Select<T> extends BasicFormElement<Select<T>, T> implements Focusable<Select<T>>, IsReadOnly<Select<T>> {

    private static final String OPEN = "open";
    private static final String CLICK_EVENT = "click";
    private static final String KEYDOWN = "keydown";
    private static final String FOCUSED = "focused";
    private static final String TOUCH_START_EVENT = "touchend";

    private HTMLDivElement container = div().css("form-group").asElement();
    private SelectElement selectElement = SelectElement.create();
    private HTMLElement leftAddonContainer = div().css("input-addon-container").asElement();
    private HTMLElement rightAddonContainer = div().css("input-addon-container").asElement();
    private LinkedHashMap<String, SelectOption<T>> searchableOptions = new LinkedHashMap<>();
    private LinkedList<SelectOption<T>> options = new LinkedList<>();
    private SelectOption<T> selectedOption;
    private List<SelectionHandler<T>> selectionHandlers = new ArrayList<>();
    private SelectionHandler<T> autoValidationHandler;
    private Color focusColor = Color.BLUE;
    private Element leftAddon;
    private Element rightAddon;
    private boolean readOnly;
    private boolean searchable = true;
    private HTMLElement defaultNoSearchResultsElement = li().css("no-results").style("display: none;").asElement();
    private HTMLElement noSearchResultsElement;
    private String noResultsElementDisplay;
    private boolean caseSensitiveSearch = false;
    private List<SelectOptionGroup<T>> groups = new ArrayList<>();
    private boolean touchMoved;
    private String noMatchSearchResultText = "No results matched";

    public Select() {
        initListeners();
        container.appendChild(leftAddonContainer);
        container.appendChild(selectElement.asElement());
        container.appendChild(rightAddonContainer);
        selectElement.getOptionsList().appendChild(defaultNoSearchResultsElement);
        selectElement.getSearchContainer().addEventListener("click", evt -> {
            evt.preventDefault();
            evt.stopPropagation();
        });
        init(this);
    }

    private void initListeners() {
        EventListener hideAllListener = this::hideAllMenus;
        document.addEventListener(CLICK_EVENT, hideAllListener);
        document.addEventListener("touchend", evt -> {
            if (!touchMoved) {
                hideAllListener.handleEvent(evt);
            }
            touchMoved = false;
        });

        document.addEventListener("touchmove", evt -> this.touchMoved = true);

        document.body.addEventListener(KEYDOWN, new NavigateOptionsKeyListener());

        EventListener clickListener = evt -> {
            open();
            evt.stopPropagation();
        };
        selectElement.getSelectButton().addEventListener(CLICK_EVENT, clickListener);
        selectElement.getSelectMenu().addEventListener("focusin", evt -> focus());
        selectElement.getSelectMenu().addEventListener("focusout", evt -> unfocus());
        selectElement.getSelectButton().addEventListener("focus", evt -> selectElement.getSelectButton().blur());
        selectElement.getSelectMenu().addEventListener(KEYDOWN, evt -> {
            KeyboardEvent keyboardEvent = (KeyboardEvent) evt;
            if (isSpaceKey(keyboardEvent) || isEnterKey(keyboardEvent) || isArrowDown(keyboardEvent) || isArrowUp(keyboardEvent)) {
                open();
                evt.preventDefault();
            }
        });

        selectElement.getSearchBox().addEventListener("input", evt -> doSearch());
        selectElement.getSearchBox().addEventListener(KEYDOWN, evt -> {
            KeyboardEvent keyboardEvent = (KeyboardEvent) evt;
            if (isArrowUp(keyboardEvent)) {
                options.getLast().focus();
                evt.preventDefault();
            } else if (isArrowDown(keyboardEvent)) {
                options.stream().filter(so -> !isHidden(so))
                        .findFirst().ifPresent(SelectOption::focus);
                evt.preventDefault();
            }
        });
    }

    private boolean isHidden(SelectOption<T> option) {
        return option.asElement().classList.contains("hidden");
    }

    private void hideAllMenus(Event evt) {
        HTMLElement element = Js.uncheckedCast(evt.target);
        if (!selectElement.getFormControl().contains(element)) {
            hideAllMenus();
        }
    }

    private void doSearch() {
        if (searchable) {
            String searchValue = selectElement.getSearchBox().asElement().value;
            boolean isThereValues = changeOptionsVisibility(searchValue);

            if (!isThereValues) {
                showNoResultsElement(searchValue);
            } else {
                hideNoResultsElement();
            }
        }
    }

    private boolean changeOptionsVisibility(String searchValue) {
        boolean isThereValues = false;
        for (Map.Entry<String, SelectOption<T>> entry : searchableOptions.entrySet()) {
            boolean contains;
            if (caseSensitiveSearch)
                contains = entry.getKey().contains(searchValue);
            else
                contains = entry.getKey().toLowerCase().contains(searchValue.toLowerCase());

            if (!contains) {
                entry.getValue().asElement().classList.add("hidden");
            } else {
                isThereValues = true;
                entry.getValue().asElement().classList.remove("hidden");
            }
        }
        groups.forEach(SelectOptionGroup::changeVisibility);
        return isThereValues;
    }

    private void showNoResultsElement(String searchValue) {
        if (isNull(noSearchResultsElement)) {
            Style.of(defaultNoSearchResultsElement).setDisplay("list-item");
            defaultNoSearchResultsElement.textContent = noMatchSearchResultText + " \"" + searchValue + "\"";
        } else {
            Style.of(noSearchResultsElement).setDisplay(noResultsElementDisplay);
        }
    }

    private void hideNoResultsElement() {
        if (isNull(noSearchResultsElement)) {
            Style.of(defaultNoSearchResultsElement).setDisplay("none");
        } else {
            Style.of(noSearchResultsElement).setDisplay("none");
        }
    }

    public Select<T> clearSearch() {
        for (SelectOption<T> option : options) {
            option.asElement().classList.remove("hidden");
        }
        selectElement.getSearchBox().asElement().value = "";
        hideNoResultsElement();
        return this;
    }

    public Select<T> open() {
        if (isEnabled() && !isReadOnly()) {
            hideAllMenus();
            doOpen();
            if (nonNull(getSelectedOption()))
                getSelectedOption().focus();
            else if (!options.isEmpty())
                options.getFirst().focus();
            if (searchable) {
                clearSearch();
            }
        }
        return this;
    }

    public Select<T> setCaseSensitiveSearch(boolean caseSensitiveSearch) {
        this.caseSensitiveSearch = caseSensitiveSearch;
        return this;
    }

    public Select(String label) {
        this();
        setLabel(label);
    }

    public Select(List<SelectOption<T>> options) {
        this("", options);
    }

    public Select(String label, List<SelectOption<T>> options) {
        this(label);
        options.forEach(this::addOption);
    }

    private void doOpen() {
        selectElement.getFormControl().style().add(OPEN);
    }

    public void hideAllMenus() {
        NodeList<Element> elementsByName = document.body
                .getElementsByClassName("bootstrap-select");
        for (int i = 0; i < elementsByName.length; i++) {
            Element item = elementsByName.item(i);
            if (item.classList.contains(OPEN))
                close(item);
        }
    }

    private void close(Element item) {
        item.classList.remove(OPEN);
        item.classList.remove("fc-" + focusColor.getStyle());
        item.querySelector(".form-control").classList.remove(FOCUSED);
        item.querySelector(".form-label").classList.remove(focusColor.getStyle());
    }

    public void close() {
        close(selectElement.getFormControl().asElement());
        selectElement.getSelectMenu().asElement().focus();
    }

    private boolean isOpened() {
        return selectElement.asElement().classList.contains(OPEN);
    }

    public static <T> Select<T> create() {
        return new Select<>();
    }

    public static <T> Select<T> create(String label) {
        return new Select<>(label);
    }

    public static <T> Select<T> create(String label, List<SelectOption<T>> options) {
        return new Select<>(label, options);
    }

    public static <T> Select create(List<SelectOption<T>> options) {
        return new Select<>(options);
    }

    public Select<T> divider() {
        selectElement.getOptionsList().appendChild(li().css("divider").asElement());
        return this;
    }

    public Select<T> addGroup(SelectOptionGroup<T> group) {
        groups.add(group);
        selectElement.getOptionsList().appendChild(group.asElement());
        group.addOptionsTo(this);
        return this;
    }

    public Select<T> addOptions(List<SelectOption<T>> options) {
        options.forEach(this::addOption);
        return this;
    }

    /**
     * @deprecated use {@link #appendChild(SelectOption)}
     */
    @Deprecated
    public Select<T> addOption(SelectOption<T> option) {
        return appendChild(option);
    }

    public Select<T> appendChild(SelectOption<T> option) {
        options.add(option);
        searchableOptions.put(option.getDisplayValue(), option);
        EventListener openOptionListener = evt -> {
            doSelectOption(option);
            evt.stopPropagation();
        };
        option.asElement().addEventListener(CLICK_EVENT, openOptionListener);
        option.asElement().addEventListener(TOUCH_START_EVENT, evt -> {
            if (!touchMoved) {
                doSelectOption(option);
                evt.preventDefault();
            }
        });
        appendOptionValue(option);
        return this;
    }

    private void doSelectOption(SelectOption<T> option) {
        if (isEnabled()) {
            select(option);
            close();
        }
    }

    private void appendOptionValue(SelectOption<T> option) {
        selectElement.getOptionsList().appendChild(option.asElement());
        selectElement.getSelectMenu().appendChild(Elements.option().attr("value", option.getKey())
                .textContent(option.getDisplayValue())
                .asElement());
    }

    public Select<T> selectAt(int index) {
        return selectAt(index, false);
    }

    public Select<T> selectAt(int index, boolean silent) {
        if (index < options.size() && index >= 0)
            select(options.get(index), silent);
        return this;
    }

    public SelectOption<T> getOptionAt(int index) {
        if (index < options.size() && index >= 0)
            return options.get(index);
        return null;
    }

    public List<SelectOption<T>> getOptions() {
        return options;
    }

    public Select<T> select(SelectOption<T> option) {
        return select(option, false);
    }

    public Select<T> select(SelectOption<T> option, boolean silent) {
        if (selectedOption != null)
            if (!option.asElement().isEqualNode(selectedOption.asElement()))
                selectedOption.deselect();
        selectElement.getSelectLabel().style().add(FOCUSED);
        this.selectedOption = option;
        option.select();
        selectElement.getSelectedValueContainer().setTextContent(option.getDisplayValue());
        if (!silent)
            onSelection(option);
        return this;
    }

    public boolean isSelected() {
        return !isEmpty();
    }

    private void onSelection(SelectOption<T> option) {
        for (SelectionHandler<T> handler : selectionHandlers)
            handler.onSelection(option);
    }

    public Select<T> addSelectionHandler(SelectionHandler<T> selectionHandler) {
        selectionHandlers.add(selectionHandler);
        return this;
    }

    public SelectOption<T> getSelectedOption() {
        return selectedOption;
    }

    @Override
    public Select<T> enable() {
        super.enable();
        selectElement.getFormControl().style().remove("disabled");
        getSelectButton().style().remove("disabled");
        getSelectMenu().style().remove("disabled");
        getLabelElement().style().removeProperty("cursor");
        return this;
    }

    @Override
    public Select<T> disable() {
        super.disable();
        selectElement.getFormControl().style().add("disabled");
        getSelectButton().style().add("disabled");
        getSelectMenu().style().add("disabled");
        getLabelElement().style().setProperty("cursor", "not-allowed");
        return this;
    }

    public Select<T> dropup() {
        selectElement.getFormControl().style().remove("dropup");
        selectElement.getFormControl().style().add("dropup");
        return this;
    }

    public Select<T> dropdown() {
        selectElement.getFormControl().style().remove("dropup");
        return this;
    }

    @Override
    public Select<T> setValue(T value) {
        return setValue(value, false);
    }

    public Select<T> setValue(T value, boolean silent) {
        for (SelectOption<T> option : getOptions()) {
            if (Objects.equals(option.getValue(), value)) {
                select(option, silent);
            }
        }
        return this;
    }

    @Override
    public T getValue() {
        return isSelected() ? getSelectedOption().getValue() : null;
    }

    @Override
    public boolean isEmpty() {
        return isNull(selectedOption);
    }

    @Override
    public Select<T> clear() {
        selectElement.getSelectLabel().style().remove(FOCUSED);
        getOptions().forEach(selectOption -> selectOption.deselect(true));
        selectedOption = null;
        selectElement.getSelectedValueContainer().setTextContent("");
        if (isAutoValidation())
            validate();
        return this;
    }

    public Select<T> setFormId(String formId) {
        getSelectMenu().setAttribute("form", formId);
        return this;
    }

    @Override
    public Select<T> invalidate(String errorMessage) {
        selectElement.getFormControl().style().remove("fc-" + focusColor.getStyle());
        selectElement.getSelectLabel().style().remove(focusColor.getStyle());
        selectElement.getFormControl().style().add("fc-" + Color.RED.getStyle());
        selectElement.getSelectLabel().style().add(Color.RED.getStyle());
        removeLeftAddonColor(focusColor);
        setLeftAddonColor(Color.RED);
        return super.invalidate(errorMessage);
    }

    @Override
    public Select<T> clearInvalid() {
        selectElement.getFormControl().style().remove("fc-" + Color.RED.getStyle());
        selectElement.getSelectLabel().style().remove(Color.RED.getStyle());
        if (isFocused()) {
            selectElement.getFormControl().style().add("fc-" + focusColor.getStyle());
            selectElement.getSelectLabel().style().add(focusColor.getStyle());
        }

        removeLeftAddonColor(Color.RED);
        return super.clearInvalid();
    }

    public Select<T> removeSelectionHandler(SelectionHandler selectionHandler) {
        if (nonNull(selectionHandler))
            selectionHandlers.remove(selectionHandler);
        return this;
    }

    @Override
    public Select<T> focus() {
        if (isEnabled() && !isReadOnly()) {
            selectElement.getSelectMenu().style().add(FOCUSED);
            selectElement.getSelectLabel().style().add(focusColor.getStyle());
            selectElement.getFormControl().style().add("fc-" + focusColor.getStyle());
            setLeftAddonColor(focusColor);
            if(!isAttached()){
                selectElement.getSelectMenu().asElement().focus();
            }else {
                onAttached(mutationRecord -> selectElement.getSelectMenu().asElement().focus());
            }
        }
        return this;
    }

    @Override
    public Select<T> unfocus() {
        selectElement.getSelectMenu().style().remove(FOCUSED);
        selectElement.getSelectLabel().style().remove(focusColor.getStyle());
        selectElement.getFormControl().style().remove("fc-" + focusColor.getStyle());
        removeLeftAddonColor(focusColor);
        return this;
    }

    private void setLeftAddonColor(Color focusColor) {
        if (leftAddon != null)
            leftAddon.classList.add(focusColor.getStyle());
    }

    private void removeLeftAddonColor(Color focusColor) {
        if (leftAddon != null)
            leftAddon.classList.remove(focusColor.getStyle());
    }

    @Override
    public boolean isFocused() {
        return selectElement.getSelectMenu().style().contains(FOCUSED);
    }

    @Override
    public Select<T> setFocusColor(Color focusColor) {
        unfocus();
        this.focusColor = focusColor;
        if (isFocused())
            focus();
        return this;
    }

    public Select<T> removeOption(SelectOption<T> option) {
        if (nonNull(option) && getOptions().contains(option)) {
            option.deselect(true);
            option.asElement().remove();
        }
        return this;
    }

    public Select<T> removeOptions(Collection<SelectOption<T>> options) {
        if (nonNull(options) && !options.isEmpty() && !this.options.isEmpty()) {
            options.forEach(this::removeOption);
        }
        return this;
    }

    public Select<T> removeAllOptions() {
        if (nonNull(options) && !options.isEmpty()) {
            options.forEach(this::removeOption);
            options.clear();
        }
        clear();
        return this;
    }

    public SelectElement getSelectElement() {
        return selectElement;
    }

    @Override
    public Select<T> setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        if (readOnly) {
            selectElement.getFormControl().style().add("readonly");
            selectElement.getSelectMenu().setAttribute("disabled", true);
            selectElement.getSelectMenu().setAttribute("readonly", true);
            selectElement.getSelectArrow().setAttributeNS(null, "style", "display: none;");
        } else {
            selectElement.getFormControl().style().remove("readonly");
            if (!asElement().classList.contains("disabled")) {
                selectElement.getSelectMenu().removeAttribute("disabled");
            }
            selectElement.getSelectMenu().removeAttribute("readonly");
            selectElement.getSelectArrow().removeAttributeNS(null, "style");
        }
        return this;
    }

    public Select<T> setNoResultsElementDisplay(String noResultsElementDisplay) {
        this.noResultsElementDisplay = noResultsElementDisplay;
        return this;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @FunctionalInterface
    public interface SelectionHandler<T> {
        void onSelection(SelectOption<T> option);
    }

    public DominoElement<HTMLButtonElement> getSelectButton() {
        return selectElement.getSelectButton();
    }

    public DominoElement<HTMLDivElement> getDropDownMenu() {
        return selectElement.getDropDownMenu();
    }

    public DominoElement<HTMLUListElement> getOptionsList() {
        return selectElement.getOptionsList();
    }

    public DominoElement<HTMLSelectElement> getSelectMenu() {
        return selectElement.getSelectMenu();
    }

    public DominoElement<HTMLElement> getSelectedValueContainer() {
        return selectElement.getSelectedValueContainer();
    }

    @Override
    public Select<T> setAutoValidation(boolean autoValidation) {
        if (autoValidation) {
            if (isNull(autoValidationHandler)) {
                autoValidationHandler = option -> validate();
                addSelectionHandler(autoValidationHandler);
            }
        } else {
            removeSelectionHandler(autoValidationHandler);
            autoValidationHandler = null;
        }
        return this;
    }

    @Override
    public boolean isAutoValidation() {
        return nonNull(autoValidationHandler);
    }


    public Select<T> setLeftAddon(IsElement leftAddon) {
        return setLeftAddon(leftAddon.asElement());
    }

    public Select<T> setLeftAddon(Element leftAddon) {
        setAddon(leftAddonContainer, this.leftAddon, leftAddon);
        this.leftAddon = leftAddon;
        return this;
    }

    public Select<T> setRightAddon(IsElement rightAddon) {
        return setRightAddon(rightAddon.asElement());
    }

    public Select<T> setRightAddon(Element rightAddon) {
        setAddon(rightAddonContainer, this.rightAddon, rightAddon);
        this.rightAddon = rightAddon;
        return this;
    }

    public Select<T> removeRightAddon() {
        if (nonNull(rightAddon)) {
            rightAddonContainer.removeChild(rightAddon);
        }
        return this;
    }

    public Select<T> removeLeftAddon() {
        if (nonNull(leftAddon)) {
            leftAddonContainer.removeChild(leftAddon);
        }
        return this;
    }

    private void setAddon(HTMLElement container, Element oldAddon, Element addon) {
        if (nonNull(oldAddon)) {
            container.removeChild(oldAddon);
        }
        if (nonNull(addon)) {
            List<String> oldClasses = new ArrayList<>(addon.classList.asList());
            for (String oldClass : oldClasses) {
                addon.classList.remove(oldClass);
            }
            oldClasses.add(0, "input-addon");
            for (String oldClass : oldClasses) {
                addon.classList.add(oldClass);
            }
            container.appendChild(addon);
        }
    }

    public List<T> getValues() {
        return options.stream().map(SelectOption::getValue).collect(Collectors.toList());
    }

    public List<String> getKeys() {
        return options.stream().map(SelectOption::getKey).collect(Collectors.toList());
    }

    public boolean containsKey(String key) {
        return getKeys().contains(key);
    }

    public boolean containsValue(T value) {
        return getValues().contains(value);
    }

    public Select<T> setSearchable(boolean searchable) {
        if (searchable) {
            selectElement.getSearchContainer()
                    .style()
                    .setDisplay("block");
        } else {
            selectElement.getSearchContainer()
                    .style()
                    .setDisplay("none");
        }
        this.searchable = searchable;
        return this;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public Select<T> setNoSearchResultsElement(HTMLElement noResultsElement) {
        this.noSearchResultsElement = noResultsElement;
        this.noResultsElementDisplay = noResultsElement.style.display;
        defaultNoSearchResultsElement.remove();
        selectElement.getOptionsList().appendChild(noResultsElement);
        return this;
    }

    public HTMLElement getNoSearchResultsElement() {
        return isNull(noSearchResultsElement) ? defaultNoSearchResultsElement : noSearchResultsElement;
    }


    @Override
    protected DominoElement<HTMLSelectElement> getInputElement() {
        return DominoElement.of(selectElement.selectMenu);
    }

    @Override
    protected DominoElement<HTMLLabelElement> getLabelElement() {
        return DominoElement.of(selectElement.selectLabel);
    }

    @Override
    protected DominoElement<HTMLDivElement> getFieldContainer() {
        return DominoElement.of(selectElement.asElement());
    }


    @Override
    public HTMLElement asElement() {
        return container;
    }

    @Templated
    public static abstract class SelectElement extends BaseDominoElement<HTMLDivElement, SelectElement> implements IsElement<HTMLDivElement> {

        @DataElement
        HTMLDivElement formControl;

        @DataElement
        HTMLButtonElement selectButton;

        @DataElement
        HTMLDivElement dropDownMenu;

        @DataElement
        HTMLUListElement optionsList;

        @DataElement
        HTMLSelectElement selectMenu;

        @DataElement
        HTMLElement selectedValueContainer;

        @DataElement
        HTMLLabelElement selectLabel;

        @DataElement
        SVGElement selectArrow;

        @DataElement
        HTMLDivElement searchContainer;

        @DataElement
        HTMLInputElement searchBox;

        @PostConstruct
        void init(){
            init(this);
        }

        public static SelectElement create() {
            return new Templated_Select_SelectElement();
        }

        public DominoElement<HTMLButtonElement> getSelectButton() {
            return DominoElement.of(selectButton);
        }

        public DominoElement<HTMLDivElement> getDropDownMenu() {
            return DominoElement.of(dropDownMenu);
        }

        public DominoElement<HTMLUListElement> getOptionsList() {
            return DominoElement.of(optionsList);
        }

        public DominoElement<HTMLSelectElement> getSelectMenu() {
            return DominoElement.of(selectMenu);
        }

        public DominoElement<HTMLElement> getSelectedValueContainer() {
            return DominoElement.of(selectedValueContainer);
        }

        public DominoElement<HTMLLabelElement> getSelectLabel() {
            return DominoElement.of(selectLabel);
        }

        public SVGElement getSelectArrow() {
            return selectArrow;
        }

        public DominoElement<HTMLDivElement> getFormControl() {
            return DominoElement.of(formControl);
        }

        public DominoElement<HTMLDivElement> getSearchContainer() {
            return DominoElement.of(searchContainer);
        }

        public DominoElement<HTMLInputElement> getSearchBox() {
            return DominoElement.of(searchBox);
        }
    }

    private final class NavigateOptionsKeyListener implements EventListener {

        @Override
        public void handleEvent(Event evt) {
            KeyboardEvent keyboardEvent = (KeyboardEvent) evt;
            HTMLElement element = Js.uncheckedCast(keyboardEvent.target);
            for (SelectOption<T> option : options) {
                if (option.asElement().contains(element)) {
                    if (isArrowUp(keyboardEvent)) {
                        focusPrev(option);
                        evt.preventDefault();
                    } else if (isArrowDown(keyboardEvent)) {
                        focusNext(option);
                        evt.preventDefault();
                    }

                    if (isEnterKey(keyboardEvent) ||
                            isSpaceKey(keyboardEvent)
                            || isKeyOf("tab", keyboardEvent)) {
                        doSelectOption(option);
                        evt.preventDefault();
                    }
                }
            }
        }

        private void focusNext(SelectOption<T> option) {
            int nextIndex = options.indexOf(option) + 1;
            int size = options.size();
            if (nextIndex >= size) {
                options.getFirst().focus();
            } else {
                for (int i = nextIndex; i < size; i++) {
                    SelectOption<T> nextOption = options.get(i);
                    if (!isHidden(nextOption)) {
                        nextOption.focus();
                        break;
                    }
                }
            }
        }

        private void focusPrev(SelectOption<T> option) {
            int nextIndex = options.indexOf(option) - 1;
            if (nextIndex < 0) {
                options.getLast().focus();
            } else {
                for (int i = nextIndex; i >= 0; i--) {
                    SelectOption<T> nextOption = options.get(i);
                    if (!isHidden(nextOption)) {
                        nextOption.focus();
                        break;
                    }
                }
            }
        }
    }

    private boolean isArrowDown(KeyboardEvent keyboardEvent) {
        return isKeyOf("ArrowDown", keyboardEvent);
    }

    private boolean isArrowUp(KeyboardEvent keyboardEvent) {
        return isKeyOf("ArrowUp", keyboardEvent);
    }
}
