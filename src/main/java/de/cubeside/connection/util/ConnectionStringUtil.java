package de.cubeside.connection.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * From StringUtilForge
 */
public class ConnectionStringUtil {
    public static final char COLOR_CHAR = '§';
    public static final Pattern COLOR_CHAR_PATTERN = Pattern.compile("\\" + COLOR_CHAR);
    public static final Pattern COLOR_CODES_PATTERN = Pattern.compile("\\" + COLOR_CHAR + "([0-9a-fk-or]|(x(" + COLOR_CHAR + "[0-9a-f]){6}))", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL = Pattern.compile("^(?:(https?)://)?([-\\w_\\.]{2,}\\.[a-z]{2,4})(/\\S*)?$");

    public static Component parseLegacyColoredString(String text) {
        return parseLegacyColoredString(text, false);
    }

    public static Component parseLegacyColoredString(String text, boolean parseUrls) {
        ArrayList<MutableComponent> components = new ArrayList<>();
        Style style = Style.EMPTY;
        Style newStyle = style;
        StringBuilder builder = new StringBuilder();
        Matcher urlMatcher = parseUrls ? URL.matcher(text) : null;

        int len = text.length();
        for (int i = 0; i < len; i++) {
            char current = text.charAt(i);
            if (current == COLOR_CHAR) {
                if (i < len) {
                    char formatting = text.charAt(i + 1);
                    ChatFormatting chatFormatting = ChatFormatting.getByCode(formatting);
                    formatingSection: if (formatting == 'x') {
                        // rgb colors
                        if (len > i + 13) {
                            StringBuilder hexString = new StringBuilder();
                            for (int j = 0; j < 6; j++) {
                                char expectedColorChar = text.charAt(i + 2 + j * 2);
                                char expectedHexColorPart = text.charAt(i + 2 + j * 2 + 1);
                                if (expectedColorChar != COLOR_CHAR) {
                                    break formatingSection;
                                }
                                expectedHexColorPart = Character.toLowerCase(expectedHexColorPart);
                                if ((expectedHexColorPart >= '0' && expectedHexColorPart <= '9') || (expectedHexColorPart >= 'a' && expectedHexColorPart <= 'f')) {
                                    hexString.append(expectedHexColorPart);
                                } else {
                                    break formatingSection;
                                }
                            }
                            int color = Integer.parseInt(hexString.toString(), 16);
                            newStyle = Style.EMPTY.withColor(color);
                            i += 13;
                        }
                    } else if (chatFormatting != null) {
                        newStyle = style.applyLegacyFormat(chatFormatting);
                        i++;
                    }
                }
                if (!newStyle.equals(style)) {
                    if (!builder.isEmpty()) {
                        components.add(Component.literal(builder.toString()).setStyle(style));
                        builder.delete(0, builder.length());
                    }
                    style = newStyle;
                }
            } else {
                if (parseUrls) {
                    // look for urls
                    int nextSpace = text.indexOf(' ', i);
                    if (nextSpace == -1) {
                        nextSpace = text.length();
                    }
                    if (urlMatcher.region(i, nextSpace).find()) {
                        if (!builder.isEmpty()) {
                            components.add(Component.literal(builder.toString()).setStyle(style));
                            builder.delete(0, builder.length());
                        }
                        String url = text.substring(i, nextSpace);
                        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, url.startsWith("http") ? url : "http://" + url);
                        components.add(Component.literal(url).setStyle(style.withClickEvent(clickEvent)));
                        i = nextSpace - 1;
                        continue;
                    }
                }
                // normal char
                builder.append(current);
            }
        }
        if (!builder.isEmpty()) {
            components.add(Component.literal(builder.toString()).setStyle(style));
        }
        if (components.isEmpty()) {
            return Component.literal("");
        } else if (components.size() == 1) {
            return components.get(0);
        } else if (components.get(0).getStyle().equals(Style.EMPTY)) {
            MutableComponent parent = components.get(0);
            for (int i = 1; i < components.size(); i++) {
                parent.append(components.get(i));
            }
            return parent;
        } else {
            MutableComponent parent = Component.literal("");
            for (int i = 0; i < components.size(); i++) {
                parent.append(components.get(i));
            }
            return parent;
        }
    }
}
