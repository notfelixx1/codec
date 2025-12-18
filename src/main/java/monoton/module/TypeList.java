package monoton.module;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TypeList {
    Combat("b"),
    Movement("c"),
    Render("d"),
    Player("e"),
    Misc("o");

    public final String icon;
}
