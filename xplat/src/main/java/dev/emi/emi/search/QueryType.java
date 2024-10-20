package dev.emi.emi.search;

import java.util.function.Function;

import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

public enum QueryType {
	DEFAULT("",  Formatting.WHITE, Formatting.RED, Formatting.RED, Formatting.GOLD, NameQuery::new, RegexNameQuery::new),
	MOD(    "@", Formatting.BLUE, Formatting.BLUE, Formatting.DARK_PURPLE, Formatting.LIGHT_PURPLE, ModQuery::new, RegexModQuery::new),
	TOOLTIP("$", Formatting.YELLOW, Formatting.YELLOW, Formatting.GREEN, Formatting.GOLD, TooltipQuery::new, RegexTooltipQuery::new),
	TAG(    "#", Formatting.GREEN, Formatting.GREEN, Formatting.AQUA, Formatting.DARK_AQUA, TagQuery::new, RegexTagQuery::new),
	;

	public final String prefix;
	public final Style color, slashColor, regexColor, escapeColor;
	public final Function<String, Query> queryConstructor, regexQueryConstructor;

	private QueryType(String prefix, Formatting color, Formatting slashColor, Formatting regexColor, Formatting escapeColor,
			Function<String, Query> queryConstructor, Function<String, Query> regexQueryConstructor) {
		this.prefix = prefix;
		this.color = new Style().setFormatting(color);
		this.slashColor = new Style().setFormatting(slashColor);
		this.regexColor = new Style().setFormatting(regexColor);
		this.escapeColor = new Style().setFormatting(escapeColor);
		this.queryConstructor = queryConstructor;
		this.regexQueryConstructor = regexQueryConstructor;
	}

	public static QueryType fromString(String s) {
		for (int i = QueryType.values().length - 1; i >= 0; i--) {
			QueryType type = QueryType.values()[i];
			if (s.startsWith(type.prefix)) {
				return type;
			}
		}
		return null;
	}
}
