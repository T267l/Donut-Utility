package donut.utility.features.networth;

import java.text.Normalizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_1887;
import net.minecraft.class_2561;
import net.minecraft.class_6880;
import net.minecraft.class_9290;
import net.minecraft.class_9304;
import net.minecraft.class_9334;

@Environment(value=EnvType.CLIENT)
public class ItemData {
    private final String itemId;
    private final String displayName;
    private final String normalizedName;
    private final Map<String, Integer> enchantments;
    private final boolean isSpawner;
    private final String spawnerType;
    private final String searchQuery;
    private static final Map<Character, Character> UNICODE_MAP = new HashMap<Character, Character>();
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public ItemData(class_1799 stack) {
        this.displayName = stack.method_7964().getString();
        this.normalizedName = ItemData.normalizeUnicode(this.displayName);
        this.itemId = this.extractItemId(stack);
        this.enchantments = this.extractEnchantments(stack);
        if (this.normalizedName.equalsIgnoreCase("Spawner") || this.itemId.contains("spawner")) {
            this.isSpawner = true;
            this.spawnerType = this.extractSpawnerType(stack);
        } else {
            this.isSpawner = false;
            this.spawnerType = null;
        }
        this.searchQuery = this.buildSearchQuery();
    }

    private static String normalizeUnicode(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (UNICODE_MAP.containsKey(Character.valueOf(c))) {
                result.append(UNICODE_MAP.get(Character.valueOf(c)));
                continue;
            }
            if (c < '\u0080') {
                result.append(c);
                continue;
            }
            String normalized = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
            String stripped = DIACRITICS.matcher(normalized).replaceAll("");
            if (!stripped.isEmpty() && stripped.charAt(0) < '\u0080') {
                result.append(stripped.charAt(0));
                continue;
            }
            result.append(c);
        }
        return result.toString();
    }

    private String extractItemId(class_1799 stack) {
        String id = stack.method_7909().toString();
        if (id.startsWith("minecraft:")) {
            id = id.substring(10);
        }
        return id;
    }

    private Map<String, Integer> extractEnchantments(class_1799 stack) {
        LinkedHashMap<String, Integer> enchants = new LinkedHashMap<String, Integer>();
        class_9304 enchantComponent = (class_9304)stack.method_58694(class_9334.field_49633);
        if (enchantComponent != null && !enchantComponent.method_57543()) {
            for (class_6880 entry : enchantComponent.method_57534()) {
                int level = enchantComponent.method_57536(entry);
                String enchantName = this.formatEnchantmentName((class_6880<class_1887>)entry);
                enchants.put(enchantName, level);
            }
        }
        return enchants;
    }

    private String formatEnchantmentName(class_6880<class_1887> entry) {
        String key = ((class_1887)entry.comp_349()).comp_2686().getString();
        return this.capitalizeWords(key);
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String[] words = str.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() <= 1) continue;
            result.append(word.substring(1).toLowerCase());
        }
        return result.toString();
    }

    private String extractSpawnerType(class_1799 stack) {
        class_9290 loreComponent = (class_9290)stack.method_58694(class_9334.field_49632);
        if (loreComponent == null) {
            return "Unknown";
        }
        List lore = loreComponent.comp_2400();
        for (class_2561 line : lore) {
            String text = line.getString().trim();
            if (text.isEmpty() || text.equalsIgnoreCase("Sets Mob Type")) continue;
            return ItemData.normalizeUnicode(text);
        }
        return "Unknown";
    }

    private String buildSearchQuery() {
        if (this.isSpawner) {
            return null;
        }
        StringBuilder query = new StringBuilder();
        query.append(this.itemId);
        for (Map.Entry<String, Integer> enchant : this.enchantments.entrySet()) {
            query.append(" ").append(enchant.getKey()).append(" ").append(enchant.getValue());
        }
        return query.toString();
    }

    public String getItemId() {
        return this.itemId;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getNormalizedName() {
        return this.normalizedName;
    }

    public Map<String, Integer> getEnchantments() {
        return Collections.unmodifiableMap(this.enchantments);
    }

    public boolean isSpawner() {
        return this.isSpawner;
    }

    public String getSpawnerType() {
        return this.spawnerType;
    }

    public String getSearchQuery() {
        return this.searchQuery;
    }

    public boolean hasEnchantments() {
        return !this.enchantments.isEmpty();
    }

    public String getDisplayString() {
        if (this.isSpawner) {
            return this.spawnerType + " Spawner";
        }
        if (this.enchantments.isEmpty()) {
            return this.displayName;
        }
        return this.displayName + " (E)";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ItemData itemData = (ItemData)o;
        return Objects.equals(this.itemId, itemData.itemId) && Objects.equals(this.enchantments, itemData.enchantments) && Objects.equals(this.spawnerType, itemData.spawnerType);
    }

    public int hashCode() {
        return Objects.hash(this.itemId, this.enchantments, this.spawnerType);
    }

    public String toString() {
        if (this.isSpawner) {
            return this.spawnerType + " Spawner";
        }
        return this.searchQuery != null ? this.searchQuery : this.displayName;
    }

    static {
        UNICODE_MAP.put(Character.valueOf('\u1d00'), Character.valueOf('a'));
        UNICODE_MAP.put(Character.valueOf('\u0299'), Character.valueOf('b'));
        UNICODE_MAP.put(Character.valueOf('\u1d04'), Character.valueOf('c'));
        UNICODE_MAP.put(Character.valueOf('\u1d05'), Character.valueOf('d'));
        UNICODE_MAP.put(Character.valueOf('\u1d07'), Character.valueOf('e'));
        UNICODE_MAP.put(Character.valueOf('\u0493'), Character.valueOf('f'));
        UNICODE_MAP.put(Character.valueOf('\u0262'), Character.valueOf('g'));
        UNICODE_MAP.put(Character.valueOf('\u029c'), Character.valueOf('h'));
        UNICODE_MAP.put(Character.valueOf('\u026a'), Character.valueOf('i'));
        UNICODE_MAP.put(Character.valueOf('\u1d0a'), Character.valueOf('j'));
        UNICODE_MAP.put(Character.valueOf('\u1d0b'), Character.valueOf('k'));
        UNICODE_MAP.put(Character.valueOf('\u029f'), Character.valueOf('l'));
        UNICODE_MAP.put(Character.valueOf('\u1d0d'), Character.valueOf('m'));
        UNICODE_MAP.put(Character.valueOf('\u0274'), Character.valueOf('n'));
        UNICODE_MAP.put(Character.valueOf('\u1d0f'), Character.valueOf('o'));
        UNICODE_MAP.put(Character.valueOf('\u1d18'), Character.valueOf('p'));
        UNICODE_MAP.put(Character.valueOf('\u01eb'), Character.valueOf('q'));
        UNICODE_MAP.put(Character.valueOf('\u0280'), Character.valueOf('r'));
        UNICODE_MAP.put(Character.valueOf('s'), Character.valueOf('s'));
        UNICODE_MAP.put(Character.valueOf('\u1d1b'), Character.valueOf('t'));
        UNICODE_MAP.put(Character.valueOf('\u1d1c'), Character.valueOf('u'));
        UNICODE_MAP.put(Character.valueOf('\u1d20'), Character.valueOf('v'));
        UNICODE_MAP.put(Character.valueOf('\u1d21'), Character.valueOf('w'));
        UNICODE_MAP.put(Character.valueOf('x'), Character.valueOf('x'));
        UNICODE_MAP.put(Character.valueOf('\u028f'), Character.valueOf('y'));
        UNICODE_MAP.put(Character.valueOf('\u1d22'), Character.valueOf('z'));
        UNICODE_MAP.put(Character.valueOf('\u2070'), Character.valueOf('0'));
        UNICODE_MAP.put(Character.valueOf('\u00b9'), Character.valueOf('1'));
        UNICODE_MAP.put(Character.valueOf('\u00b2'), Character.valueOf('2'));
        UNICODE_MAP.put(Character.valueOf('\u00b3'), Character.valueOf('3'));
        UNICODE_MAP.put(Character.valueOf('\u2074'), Character.valueOf('4'));
        UNICODE_MAP.put(Character.valueOf('\u2075'), Character.valueOf('5'));
        UNICODE_MAP.put(Character.valueOf('\u2076'), Character.valueOf('6'));
        UNICODE_MAP.put(Character.valueOf('\u2077'), Character.valueOf('7'));
        UNICODE_MAP.put(Character.valueOf('\u2078'), Character.valueOf('8'));
        UNICODE_MAP.put(Character.valueOf('\u2079'), Character.valueOf('9'));
        UNICODE_MAP.put(Character.valueOf('\u2080'), Character.valueOf('0'));
        UNICODE_MAP.put(Character.valueOf('\u2081'), Character.valueOf('1'));
        UNICODE_MAP.put(Character.valueOf('\u2082'), Character.valueOf('2'));
        UNICODE_MAP.put(Character.valueOf('\u2083'), Character.valueOf('3'));
        UNICODE_MAP.put(Character.valueOf('\u2084'), Character.valueOf('4'));
        UNICODE_MAP.put(Character.valueOf('\u2085'), Character.valueOf('5'));
        UNICODE_MAP.put(Character.valueOf('\u2086'), Character.valueOf('6'));
        UNICODE_MAP.put(Character.valueOf('\u2087'), Character.valueOf('7'));
        UNICODE_MAP.put(Character.valueOf('\u2088'), Character.valueOf('8'));
        UNICODE_MAP.put(Character.valueOf('\u2089'), Character.valueOf('9'));
    }
}
